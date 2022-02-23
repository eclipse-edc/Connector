/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import de.fraunhofer.iais.eis.ContractAgreementMessage;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.util.MessageFactory;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.ContractTransformerInput;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResult;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * This class handles and processes incoming IDS {@link ContractAgreementMessage}s.
 */
public class ContractAgreementHandler implements Handler {

    private final Monitor monitor;
    private final Serializer serializer;
    private final ConsumerContractNegotiationManager negotiationManager;
    private final TransformerRegistry transformerRegistry;
    private final MessageFactory messageFactory;

    public ContractAgreementHandler(
            @NotNull Monitor monitor,
            @NotNull Serializer serializer,
            @NotNull ConsumerContractNegotiationManager negotiationManager,
            @NotNull TransformerRegistry transformerRegistry,
            @NotNull MessageFactory messageFactory) {
        this.monitor = Objects.requireNonNull(monitor);
        this.serializer = Objects.requireNonNull(serializer);
        this.negotiationManager = Objects.requireNonNull(negotiationManager);
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
        this.messageFactory = Objects.requireNonNull(messageFactory);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof ContractAgreementMessage;
    }

    @Override
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull Result<ClaimToken> verificationResult) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(verificationResult);

        var message = (ContractAgreementMessage) multipartRequest.getHeader();

        de.fraunhofer.iais.eis.ContractAgreement contractAgreement;
        try {
            contractAgreement = serializer.deserialize(multipartRequest.getPayload(), de.fraunhofer.iais.eis.ContractAgreement.class);
        } catch (IOException e) {
            throw new EdcException(e);
        }

        // extract target from contract request
        var permission = contractAgreement.getPermission().get(0);
        if (permission == null) {
            monitor.debug("ContractAgreementHandler: Contract Agreement is invalid");
            return createBadParametersErrorMultipartResponse(message);
        }

        // search for matching asset
        // TODO remove fake asset (description request to fetch original metadata --> store/cache)
        var asset = Asset.Builder.newInstance().id(String.valueOf(permission.getTarget())).build();

        // Create contract offer request
        var input = ContractTransformerInput.Builder.newInstance()
                .contract(contractAgreement)
                .asset(asset)
                .build();

        // Create contract agreement
        Result<ContractAgreement> result = transformerRegistry.transform(input, ContractAgreement.class);
        if (result.failed()) {
            monitor.debug(String.format("Could not transform contract agreement: [%s]",
                    String.join(", ", result.getFailureMessages())));
            return createBadParametersErrorMultipartResponse(message);
        }

        // TODO get hash from message
        var agreement = result.getContent();
        var contractOfferMessageUri = message.getCorrelationMessage();
        var contractOfferMessageIdsId = IdsIdParser.parse(contractOfferMessageUri.toString());
        if (contractOfferMessageIdsId.getType() != IdsType.MESSAGE) {
            monitor.debug("ContractAgreementHandler: Correlation message ID should be of type IdsType.MESSAGE");
            return createBadParametersErrorMultipartResponse(message);
        }

        var contractOfferMessageId = contractOfferMessageIdsId.getValue();
        var negotiationResponse = negotiationManager.confirmed(verificationResult.getContent(),
                contractOfferMessageId, agreement, null);
        if (negotiationResponse.failed() && negotiationResponse.getStatus() == NegotiationResult.Status.FATAL_ERROR) {
            monitor.debug("ContractAgreementHandler: Could not process contract agreement");
            return createBadParametersErrorMultipartResponse(message);
        }

        return MultipartResponse.Builder.newInstance()
                .header(messageFactory.createMessageProcessedNotificationMessage(message))
                .build();
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(messageFactory.badParameters(message))
                .build();
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message, String payload) {
        return MultipartResponse.Builder.newInstance()
                .header(messageFactory.badParameters(message))
                .payload(payload)
                .build();
    }
}
