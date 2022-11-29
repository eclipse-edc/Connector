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

package org.eclipse.edc.connector.dataplane.transfer.proxy;

import org.eclipse.edc.connector.dataplane.transfer.spi.proxy.DataPlaneTransferProxyCreationRequest;
import org.eclipse.edc.connector.dataplane.transfer.spi.proxy.DataProxyReferenceService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReferenceMessage;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;

public class DataProxyServiceImpl implements DataProxyService {

    private final DataProxyResolver proxyResolver;
    private final DataProxyReferenceService proxyReferenceService;
    private final String connectorId;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;

    public DataProxyServiceImpl(DataProxyResolver proxyResolver, DataProxyReferenceService proxyReferenceService, String connectorId, RemoteMessageDispatcherRegistry dispatcherRegistry) {
        this.proxyResolver = proxyResolver;
        this.proxyReferenceService = proxyReferenceService;
        this.connectorId = connectorId;
        this.dispatcherRegistry = dispatcherRegistry;
    }

    @Override
    public StatusResult<Void> createProxyReferenceAndDispatch(DataRequest dataRequest, DataAddress contentAddress) {
        var proxyUrl = proxyResolver.resolveProxyUrl(contentAddress);
        if (proxyUrl.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, format("Failed to resolve proxy url for data request %s%n %s", dataRequest.getId(), proxyUrl.getFailureDetail()));
        }

        var proxyCreationRequest = DataPlaneTransferProxyCreationRequest.Builder.newInstance()
                .id(dataRequest.getId())
                .contentAddress(contentAddress)
                .proxyEndpoint(proxyUrl.getContent())
                .contractId(dataRequest.getContractId())
                .build();

        var proxyCreationResult = proxyReferenceService.createProxyReference(proxyCreationRequest);
        if (proxyCreationResult.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to generate proxy: " + proxyCreationResult.getFailureDetail());
        }
        return dispatch(proxyCreationResult.getContent(), dataRequest);
    }

    private StatusResult<Void> dispatch(@NotNull EndpointDataReference edr, @NotNull DataRequest dataRequest) {
        var request = EndpointDataReferenceMessage.Builder.newInstance()
                .connectorId(connectorId)
                .connectorAddress(dataRequest.getConnectorAddress())
                .protocol(dataRequest.getProtocol())
                .endpointDataReference(edr)
                .build();

        return dispatcherRegistry.send(Object.class, request, dataRequest::getId)
                .thenApply(o -> StatusResult.success())
                .exceptionally(throwable -> StatusResult.failure(ResponseStatus.ERROR_RETRY, "Transfer failed: " + throwable.getMessage()))
                .join();
    }
}
