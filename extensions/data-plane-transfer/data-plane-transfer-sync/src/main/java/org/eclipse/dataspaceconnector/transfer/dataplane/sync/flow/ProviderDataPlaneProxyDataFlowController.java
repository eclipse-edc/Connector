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

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceMessage;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyManager;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.dataspaceconnector.transfer.dataplane.spi.DataPlaneTransferType.SYNC;

public class ProviderDataPlaneProxyDataFlowController implements DataFlowController {

    private final String connectorId;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;
    private final DataPlaneProxyManager proxyManager;

    public ProviderDataPlaneProxyDataFlowController(String connectorId, RemoteMessageDispatcherRegistry dispatcherRegistry, DataPlaneProxyManager proxyManager) {
        this.connectorId = connectorId;
        this.dispatcherRegistry = dispatcherRegistry;
        this.proxyManager = proxyManager;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest, DataAddress contentAddress) {
        return SYNC.equals(dataRequest.getDestinationType());
    }

    @Override
    public @NotNull StatusResult<String> initiateFlow(DataRequest dataRequest, DataAddress contentAddress, Policy policy) {
        var proxyCreationRequest = DataPlaneProxyCreationRequest.Builder.newInstance()
                .id(dataRequest.getId())
                .address(contentAddress)
                .contractId(dataRequest.getContractId())
                .build();
        var proxyCreationResult = proxyManager.createProxy(proxyCreationRequest);
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
                .thenApply(o -> StatusResult.success("Transfer successful"))
                .exceptionally(throwable -> StatusResult.failure(ResponseStatus.ERROR_RETRY, "Transfer failed: " + throwable.getMessage()))
                .join();
    }
}
