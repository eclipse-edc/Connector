/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.transfer.sync.flow;

import org.eclipse.edc.connector.dataplane.transfer.spi.proxy.DataPlaneTransferProxyCreationRequest;
import org.eclipse.edc.connector.dataplane.transfer.spi.proxy.DataPlaneTransferProxyReferenceService;
import org.eclipse.edc.connector.dataplane.transfer.sync.proxy.DataPlaneTransferProxyResolver;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReferenceMessage;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;
import static org.eclipse.edc.connector.dataplane.transfer.spi.DataPlaneTransferConstants.HTTP_PROXY;

public class ProviderDataPlaneProxyDataFlowController implements DataFlowController {

    private final String connectorId;
    private final DataPlaneTransferProxyResolver proxyResolver;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;
    private final DataPlaneTransferProxyReferenceService proxyReferenceService;

    public ProviderDataPlaneProxyDataFlowController(String connectorId,
                                                    DataPlaneTransferProxyResolver proxyResolver,
                                                    RemoteMessageDispatcherRegistry dispatcherRegistry,
                                                    DataPlaneTransferProxyReferenceService proxyReferenceService) {
        this.connectorId = connectorId;
        this.proxyResolver = proxyResolver;
        this.dispatcherRegistry = dispatcherRegistry;
        this.proxyReferenceService = proxyReferenceService;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest, DataAddress contentAddress) {
        return HTTP_PROXY.equals(dataRequest.getDestinationType());
    }

    @Override
    public @NotNull StatusResult<Void> initiateFlow(DataRequest dataRequest, DataAddress contentAddress, Policy policy) {
        var proxyUrl = proxyResolver.resolveProxyUrl(contentAddress);
        if (proxyUrl.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR,
                    format("Failed to resolve proxy url for data request %s%n %s", dataRequest.getId(), String.join(",", proxyUrl.getFailureMessages())));
        }

        var proxyCreationRequest = DataPlaneTransferProxyCreationRequest.Builder.newInstance()
                .id(dataRequest.getId())
                .contentAddress(contentAddress)
                .proxyEndpoint(proxyUrl.getContent())
                .contractId(dataRequest.getContractId())
                .build();
        var proxyCreationResult = proxyReferenceService.createProxyReference(proxyCreationRequest);
        if (proxyCreationResult.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to generate proxy: " + String.join(", ", proxyCreationResult.getFailureMessages()));
        }

        var request = EndpointDataReferenceMessage.Builder.newInstance()
                .connectorId(connectorId)
                .connectorAddress(dataRequest.getConnectorAddress())
                .protocol(dataRequest.getProtocol())
                .endpointDataReference(proxyCreationResult.getContent())
                .build();

        return dispatcherRegistry.send(Object.class, request, dataRequest::getId)
                .thenApply(o -> StatusResult.success())
                .exceptionally(throwable -> StatusResult.failure(ResponseStatus.ERROR_RETRY, "Transfer failed: " + throwable.getMessage()))
                .join();
    }
}
