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

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.flow;

import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResult;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.schema.HttpProxySchema;
import org.jetbrains.annotations.NotNull;

public class HttpProviderProxyDataFlowController implements DataFlowController {

    private static final String DATA_PLANE_AUTH_KEY = "Authorization";

    private final String connectorId;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;

    public HttpProviderProxyDataFlowController(String connectorId, RemoteMessageDispatcherRegistry dispatcherRegistry) {
        this.connectorId = connectorId;
        this.dispatcherRegistry = dispatcherRegistry;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return dataRequest.getDataDestination() != null &&
                HttpProxySchema.TYPE.equals(dataRequest.getDataDestination().getType()) &&
                dataRequest.getDataDestination().getProperties().containsKey(HttpProxySchema.ENDPOINT) &&
                dataRequest.getDataDestination().getProperties().containsKey(HttpProxySchema.TOKEN) &&
                dataRequest.getDataDestination().getProperties().containsKey(HttpProxySchema.EXPIRATION);
    }

    @Override
    public @NotNull DataFlowInitiateResult initiateFlow(DataRequest dataRequest) {
        var edr = createEndpointDataReference(dataRequest);
        var request = EndpointDataReferenceRequest.Builder.newInstance()
                .connectorId(connectorId)
                .connectorAddress(dataRequest.getConnectorAddress())
                .protocol(dataRequest.getProtocol())
                .endpointDataReference(edr)
                .build();

        return dispatcherRegistry.send(Object.class, request, dataRequest::getId)
                .thenApply(o -> DataFlowInitiateResult.success("Transfer successful"))
                .exceptionally(throwable -> DataFlowInitiateResult.failure(ResponseStatus.ERROR_RETRY, "Transfer failed: " + throwable.getMessage()))
                .join();
    }


    /**
     * Map the {@link DataRequest} to an {@link EndpointDataReference}.
     */
    private EndpointDataReference createEndpointDataReference(DataRequest dataRequest) {
        var proxyDataAddress = dataRequest.getDataDestination();
        return EndpointDataReference.Builder.newInstance()
                .contractId(dataRequest.getContractId())
                .correlationId(dataRequest.getId())
                .address(proxyDataAddress.getProperties().get(HttpProxySchema.ENDPOINT))
                .authKey(DATA_PLANE_AUTH_KEY)
                .authCode(proxyDataAddress.getProperties().get(HttpProxySchema.TOKEN))
                .expirationEpochSeconds(Long.parseLong(proxyDataAddress.getProperty(HttpProxySchema.EXPIRATION)))
                .build();
    }
}
