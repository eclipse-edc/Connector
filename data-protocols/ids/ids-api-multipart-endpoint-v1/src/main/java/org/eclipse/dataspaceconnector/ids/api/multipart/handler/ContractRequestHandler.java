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

import de.fraunhofer.iais.eis.ContractRequestMessage;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.util.MessageFactory;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.Protocols;
import org.eclipse.dataspaceconnector.ids.spi.transform.ContractTransformerInput;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferMessage;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * This class handles and processes incoming IDS {@link ContractRequestMessage}s.
 */
public class ContractRequestHandler implements Handler {

    private final Monitor monitor;
    private final Serializer serializer;
    private final ProviderContractNegotiationManager negotiationManager;
    private final TransformerRegistry transformerRegistry;
    private final AssetIndex assetIndex;
    private final MessageFactory messageFactory;

    public ContractRequestHandler(
            @NotNull Monitor monitor,
            @NotNull Serializer serializer,
            @NotNull ProviderContractNegotiationManager negotiationManager,
            @NotNull TransformerRegistry transformerRegistry,
            @NotNull AssetIndex assetIndex,
            @NotNull MessageFactory messageFactory) {
        this.monitor = Objects.requireNonNull(monitor);
        this.serializer = Objects.requireNonNull(serializer);
        this.negotiationManager = Objects.requireNonNull(negotiationManager);
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
        this.assetIndex = Objects.requireNonNull(assetIndex);
        this.messageFactory = Objects.requireNonNull(messageFactory);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof ContractRequestMessage;
    }

    @Override
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull Result<ClaimToken> verificationResult) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(verificationResult);

        var message = multipartRequest.getHeader();

        de.fraunhofer.iais.eis.ContractRequest contractRequest;
        try {
            contractRequest = serializer.deserialize(multipartRequest.getPayload(), de.fraunhofer.iais.eis.ContractRequest.class);
        } catch (IOException e) {
            monitor.severe("ContractRequestHandler: Contract Request is invalid", e);
            return createBadParametersErrorMultipartResponse(message);
        }

        var idsWebhookAddress = message.getSenderAgent();
        if (idsWebhookAddress == null || idsWebhookAddress.toString().isBlank()) {
            var msg = "Ids webhook address is invalid";
            monitor.debug(String.format("ContractRequestHandler: %s", msg));
            return createBadParametersErrorMultipartResponse(message, msg);
        }

        // extract target from contract request
        var permission = contractRequest.getPermission().get(0);
        if (permission == null) {
            monitor.debug("ContractRequestHandler: Contract Request is invalid");
            return createBadParametersErrorMultipartResponse(message);
        }

        var target = permission.getTarget();
        if (target == null || String.valueOf(target).isBlank()) {
            monitor.debug("ContractRequestHandler: Contract Request is invalid");
            return createBadParametersErrorMultipartResponse(message);
        }

        // search for matching asset
        var assetId = IdsIdParser.parse(String.valueOf(target));
        var asset = assetIndex.findById(assetId.getValue());
        if (asset == null) {
            var msg = "Target id is invalid";
            monitor.debug(String.format("ContractRequestHandler: %s", msg));
            return createBadParametersErrorMultipartResponse(message, msg);
        }

        // Create contract offer request
        var input = ContractTransformerInput.Builder.newInstance()
                .contract(contractRequest)
                .asset(asset)
                .build();

        Result<ContractOffer> result = transformerRegistry.transform(input, ContractOffer.class);
        if (result.failed()) {
            monitor.debug(String.format("Could not transform contract request: [%s]",
                    String.join(", ", result.getFailureMessages())));
            return createBadParametersErrorMultipartResponse(message);
        }

        var msgIdsId = IdsIdParser.parse(message.getId().toString());
        if (msgIdsId.getType() != IdsType.MESSAGE) {
            monitor.debug("ContractRequestHandler: Message ID should be of type IdsType.MESSAGE");
            return createBadParametersErrorMultipartResponse(message);
        }

        var originalOffer = result.getContent();
        var offerWithMsgId = ContractOffer.Builder
                .copy(originalOffer)
                .property(ContractOffer.PROPERTY_MESSAGE_ID, msgIdsId.getValue())
                .build();

        var requestObj = ContractOfferMessage.Builder.newInstance()
                .protocol(Protocols.IDS_MULTIPART)
                .connectorAddress(idsWebhookAddress.toString())
                .type(ContractOfferMessage.Type.INITIAL)
                .connectorId(String.valueOf(message.getIssuerConnector()))
                .contractOffer(offerWithMsgId)
                .build();

        // Start negotiation process
        negotiationManager.requested(verificationResult.getContent(), requestObj);

        return MultipartResponse.Builder.newInstance()
                .header(messageFactory.createRequestInProcessMessage(message))
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
