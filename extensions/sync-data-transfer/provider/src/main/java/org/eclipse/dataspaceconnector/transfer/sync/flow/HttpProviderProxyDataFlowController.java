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

package org.eclipse.dataspaceconnector.transfer.sync.flow;

import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResult;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.dataspaceconnector.transfer.sync.schema.HttpProxySchema.AUTH_CODE;
import static org.eclipse.dataspaceconnector.transfer.sync.schema.HttpProxySchema.AUTH_KEY;
import static org.eclipse.dataspaceconnector.transfer.sync.schema.HttpProxySchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.transfer.sync.schema.HttpProxySchema.EXPIRATION;
import static org.eclipse.dataspaceconnector.transfer.sync.schema.HttpProxySchema.TYPE;

public class HttpProviderProxyDataFlowController implements DataFlowController {

    private final String connectorId;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;

    public HttpProviderProxyDataFlowController(@NotNull String connectorId, @NotNull RemoteMessageDispatcherRegistry dispatcherRegistry) {
        this.connectorId = connectorId;
        this.dispatcherRegistry = dispatcherRegistry;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return dataRequest.isSync() && dataRequest.getDataDestination() != null && TYPE.equals(dataRequest.getDataDestination().getType());
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
    private static EndpointDataReference createEndpointDataReference(DataRequest dataRequest) {
        var proxyDataAddress = dataRequest.getDataDestination();
        return EndpointDataReference.Builder.newInstance()
                .contractId(dataRequest.getContractId())
                .correlationId(dataRequest.getId())
                .address(proxyDataAddress.getProperties().get(ENDPOINT))
                .authKey(proxyDataAddress.getProperties().get(AUTH_KEY))
                .authCode(proxyDataAddress.getProperties().get(AUTH_CODE))
                .expirationEpochSeconds(Long.parseLong(proxyDataAddress.getProperty(EXPIRATION)))
                .build();
    }
}
