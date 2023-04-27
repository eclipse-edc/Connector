/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering, Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation, refactoring
 *       Daimler TSS GmbH - introduce factory to create RequestInProcessMessage
 *
 */

package org.eclipse.edc.protocol.ids.api.multipart.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ContractRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartRequest;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartResponse;
import org.eclipse.edc.protocol.ids.spi.transform.ContractTransformerInput;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.MessageProtocol;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.badParameters;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.createMultipartResponse;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.inProcessFromServiceResult;
import static org.eclipse.edc.protocol.ids.spi.domain.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;

/**
 * This class handles and processes incoming IDS {@link de.fraunhofer.iais.eis.ContractRequestMessage}s.
 */
public class ContractRequestHandler implements Handler {

    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final IdsId connectorId;
    private final TypeTransformerRegistry transformerRegistry;
    private final AssetIndex assetIndex;
    private final ContractNegotiationProtocolService service;

    public ContractRequestHandler(
            Monitor monitor, IdsId connectorId, ObjectMapper objectMapper,
            TypeTransformerRegistry transformerRegistry,
            AssetIndex assetIndex, ContractNegotiationProtocolService service) {
        this.monitor = monitor;
        this.connectorId = connectorId;
        this.objectMapper = objectMapper;
        this.transformerRegistry = transformerRegistry;
        this.assetIndex = assetIndex;
        this.service = service;
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        return multipartRequest.getHeader() instanceof de.fraunhofer.iais.eis.ContractRequestMessage;
    }

    @Override
    public @NotNull MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest) {
        var claimToken = multipartRequest.getClaimToken();
        var message = (de.fraunhofer.iais.eis.ContractRequestMessage) multipartRequest.getHeader();

        ContractRequest contractRequest;
        try {
            contractRequest = objectMapper.readValue(multipartRequest.getPayload(), ContractRequest.class);
        } catch (IOException e) {
            monitor.severe("ContractRequestHandler: Contract Request is invalid", e);
            return createMultipartResponse(badParameters(message, connectorId));
        }

        var idsWebhookAddress = message.getProperties().get(IDS_WEBHOOK_ADDRESS_PROPERTY);
        if (idsWebhookAddress == null || idsWebhookAddress.toString().isBlank()) {
            var msg = "Ids webhook address is invalid";
            monitor.debug(String.format("ContractRequestHandler: %s", msg));
            return createMultipartResponse(badParameters(message, connectorId), msg);
        }

        var permission = contractRequest.getPermission().stream().findFirst().orElse(null);
        if (permission == null) {
            monitor.debug("ContractRequestHandler: Contract Request is invalid");
            return createMultipartResponse(badParameters(message, connectorId));
        }

        var target = permission.getTarget();
        if (target == null || String.valueOf(target).isBlank()) {
            monitor.debug("ContractRequestHandler: Contract Request is invalid");
            return createMultipartResponse(badParameters(message, connectorId));
        }

        var assetResult = IdsId.from(String.valueOf(target));
        if (assetResult.failed()) {
            var msg = "Target id is missing";
            monitor.debug(String.format("ContractRequestHandler: %s", msg));
            return createMultipartResponse(badParameters(message, connectorId), msg);
        }

        var assetId = assetResult.getContent();
        var asset = assetIndex.findById(assetId.getValue());
        if (asset == null) {
            var msg = "Target id is invalid";
            monitor.debug(String.format("ContractRequestHandler: %s", msg));
            return createMultipartResponse(badParameters(message, connectorId), msg);
        }

        var input = ContractTransformerInput.Builder.newInstance()
                .contract(contractRequest)
                .asset(asset)
                .build();

        var result = transformerRegistry.transform(input, ContractOffer.class);
        if (result.failed()) {
            monitor.debug(String.format("Could not transform contract request: [%s]",
                    String.join(", ", result.getFailureMessages())));
            return createMultipartResponse(badParameters(message, connectorId));
        }

        var contractOffer = result.getContent();
        var requestObj = ContractRequestMessage.Builder.newInstance()
                .protocol(MessageProtocol.IDS_MULTIPART)
                .callbackAddress(idsWebhookAddress.toString())
                .type(ContractRequestMessage.Type.INITIAL)
                .connectorId(String.valueOf(message.getIssuerConnector()))
                .processId(String.valueOf(message.getTransferContract()))
                .contractOffer(contractOffer)
                .build();

        var negotiationInitiateResult = service.notifyRequested(requestObj, claimToken);

        return createMultipartResponse(inProcessFromServiceResult(negotiationInitiateResult, message, connectorId));
    }
}
