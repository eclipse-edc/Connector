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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ContractAgreementMessage;
import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.transform.ContractAgreementTransformerOutput;
import org.eclipse.dataspaceconnector.ids.spi.transform.ContractTransformerInput;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.badParameters;

/**
 * This class handles and processes incoming IDS {@link ContractAgreementMessage}s.
 */
public class ContractAgreementHandler implements Handler {

    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final String connectorId;
    private final ConsumerContractNegotiationManager negotiationManager;
    private final IdsTransformerRegistry transformerRegistry;

    public ContractAgreementHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull ObjectMapper objectMapper,
            @NotNull ConsumerContractNegotiationManager negotiationManager,
            @NotNull IdsTransformerRegistry transformerRegistry) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorId = Objects.requireNonNull(connectorId);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.negotiationManager = Objects.requireNonNull(negotiationManager);
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof ContractAgreementMessage;
    }

    @Override
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull ClaimToken claimToken) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(claimToken);

        var message = (ContractAgreementMessage) multipartRequest.getHeader();

        de.fraunhofer.iais.eis.ContractAgreement contractAgreement;
        try {
            contractAgreement = objectMapper.readValue(multipartRequest.getPayload(), de.fraunhofer.iais.eis.ContractAgreement.class);
        } catch (IOException e) {
            monitor.severe("ContractAgreementHandler: Contract Agreement is invalid", e);
            return createBadParametersErrorMultipartResponse(message);
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

        var result = transformerRegistry.transform(input, ContractAgreementTransformerOutput.class);
        if (result.failed()) {
            monitor.debug(String.format("Could not transform contract agreement: [%s]",
                    String.join(", ", result.getFailureMessages())));
            return createBadParametersErrorMultipartResponse(message);
        }

        // TODO get hash from message
        var output = result.getContent();
        var processId = message.getTransferContract();
        var negotiationResponse = negotiationManager.confirmed(claimToken,
                String.valueOf(processId), output.getContractAgreement(), output.getPolicy());
        if (negotiationResponse.fatalError()) {
            monitor.debug("ContractAgreementHandler: Could not process contract agreement " + negotiationResponse.getFailureMessages());
            return createBadParametersErrorMultipartResponse(message);
        }

        return MultipartResponse.Builder.newInstance()
                .header(ResponseMessageUtil.createMessageProcessedNotificationMessage(connectorId, message))
                .build();
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .build();
    }

}
