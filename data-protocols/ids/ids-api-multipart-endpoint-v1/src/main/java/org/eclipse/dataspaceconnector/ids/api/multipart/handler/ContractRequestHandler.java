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
import de.fraunhofer.iais.eis.ContractRequest;
import de.fraunhofer.iais.eis.ContractRequestMessage;
import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.Protocols;
import org.eclipse.dataspaceconnector.ids.spi.transform.ContractTransformerInput;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.badParameters;
import static org.eclipse.dataspaceconnector.ids.spi.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;

/**
 * This class handles and processes incoming IDS {@link ContractRequestMessage}s.
 */
public class ContractRequestHandler implements Handler {

    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final String connectorId;
    private final ProviderContractNegotiationManager negotiationManager;
    private final IdsTransformerRegistry transformerRegistry;
    private final AssetIndex assetIndex;

    public ContractRequestHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull ObjectMapper objectMapper,
            @NotNull ProviderContractNegotiationManager negotiationManager,
            @NotNull IdsTransformerRegistry transformerRegistry,
            @NotNull AssetIndex assetIndex) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorId = Objects.requireNonNull(connectorId);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.negotiationManager = Objects.requireNonNull(negotiationManager);
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
        this.assetIndex = Objects.requireNonNull(assetIndex);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof ContractRequestMessage;
    }

    @Override
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull ClaimToken claimToken) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(claimToken);

        var message = (ContractRequestMessage) multipartRequest.getHeader();

        ContractRequest contractRequest;
        try {
            contractRequest = objectMapper.readValue(multipartRequest.getPayload(), ContractRequest.class);
        } catch (IOException e) {
            monitor.severe("ContractRequestHandler: Contract Request is invalid", e);
            return createBadParametersErrorMultipartResponse(message);
        }

        var idsWebhookAddress = message.getProperties().get(IDS_WEBHOOK_ADDRESS_PROPERTY);
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

        var contractOffer = result.getContent();
        var requestObj = ContractOfferRequest.Builder.newInstance()
                .protocol(Protocols.IDS_MULTIPART)
                .connectorAddress(idsWebhookAddress.toString())
                .type(ContractOfferRequest.Type.INITIAL)
                .connectorId(String.valueOf(message.getIssuerConnector()))
                .correlationId(String.valueOf(message.getTransferContract()))
                .contractOffer(contractOffer)
                .build();

        // Start negotiation process
        negotiationManager.requested(claimToken, requestObj);

        return MultipartResponse.Builder.newInstance()
                .header(ResponseMessageUtil.createRequestInProcessMessage(connectorId, message))
                .build();
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .build();
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message, String payload) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .payload(payload)
                .build();
    }
}
