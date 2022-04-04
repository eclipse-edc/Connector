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
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResult;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceMessage;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyManager;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.dataspaceconnector.transfer.dataplane.spi.DataPlaneTransferType.SYNC;

public class ProviderDataPlaneProxyDataFlowController implements DataFlowController {

    private final String connectorId;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;
    private final DataAddressResolver resolver;
    private final DataPlaneProxyManager proxyManager;

    public ProviderDataPlaneProxyDataFlowController(String connectorId,
                                                    RemoteMessageDispatcherRegistry dispatcherRegistry,
                                                    DataAddressResolver resolver,
                                                    DataPlaneProxyManager proxyManager) {
        this.connectorId = connectorId;
        this.dispatcherRegistry = dispatcherRegistry;
        this.resolver = resolver;
        this.proxyManager = proxyManager;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return SYNC.equals(dataRequest.getDestinationType());
    }

    @Override
    public @NotNull DataFlowInitiateResult initiateFlow(DataRequest dataRequest, Policy policy) {
        var address = resolver.resolveForAsset(dataRequest.getAssetId());
        var proxyCreationRequest = DataPlaneProxyCreationRequest.Builder.newInstance()
                .id(dataRequest.getId())
                .address(address)
                .contractId(dataRequest.getContractId())
                .build();
        var proxyCreationResult = proxyManager.createProxy(proxyCreationRequest);
        if (proxyCreationResult.failed()) {
            return DataFlowInitiateResult.failure(ResponseStatus.FATAL_ERROR, "Failed to generate proxy: " + String.join(", ", proxyCreationResult.getFailureMessages()));
        }

        var request = EndpointDataReferenceMessage.Builder.newInstance()
                .connectorId(connectorId)
                .connectorAddress(dataRequest.getConnectorAddress())
                .protocol(dataRequest.getProtocol())
                .endpointDataReference(proxyCreationResult.getContent())
                .build();

        return dispatcherRegistry.send(Object.class, request, dataRequest::getId)
                .thenApply(o -> DataFlowInitiateResult.success("Transfer successful"))
                .exceptionally(throwable -> DataFlowInitiateResult.failure(ResponseStatus.ERROR_RETRY, "Transfer failed: " + throwable.getMessage()))
                .join();
    }
}
