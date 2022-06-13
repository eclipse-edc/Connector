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

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.proxy;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyReferenceService;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;

/**
 * Transforms {@link EndpointDataReference} returned by the provider Control Plane in such a way that
 * the consumer Data Plane becomes a Data Proxy to query data.
 * This implies that the data query should first hit the consumer Data Plane, which then forward the
 * call to the provider Data Plane, which finally reach the actual data source.
 */
public class DataPlaneTransferConsumerProxyTransformer implements EndpointDataReferenceTransformer {

    private final String proxyEndpoint;
    private final DataPlaneTransferProxyReferenceService proxyReferenceCreator;

    public DataPlaneTransferConsumerProxyTransformer(String proxyEndpoint, DataPlaneTransferProxyReferenceService proxyCreator) {
        this.proxyEndpoint = proxyEndpoint;
        this.proxyReferenceCreator = proxyCreator;
    }

    @Override
    public boolean canHandle(@NotNull EndpointDataReference edr) {
        return true;
    }

    /**
     * Use the DPF public API as proxy to another DPF proxy, i.e. consumer Data Plane.
     */
    @Override
    public Result<EndpointDataReference> transform(@NotNull EndpointDataReference edr) {
        var address = toHttpDataAddress(edr);
        var contractId = edr.getProperties().get(CONTRACT_ID);
        if (contractId == null) {
            return Result.failure(String.format("Cannot transform endpoint data reference with id %s as contract id is missing", edr.getId()));
        }

        var builder = DataPlaneTransferProxyCreationRequest.Builder.newInstance()
                .id(edr.getId())
                .contentAddress(address)
                .proxyEndpoint(proxyEndpoint)
                .contractId(contractId);
        edr.getProperties().forEach(builder::property);
        return proxyReferenceCreator.createProxyReference(builder.build());
    }

    private static DataAddress toHttpDataAddress(EndpointDataReference edr) {
        return HttpDataAddress.Builder.newInstance()
                .baseUrl(edr.getEndpoint())
                .authKey(edr.getAuthKey())
                .authCode(edr.getAuthCode())
                .proxyBody(Boolean.TRUE.toString())
                .proxyPath(Boolean.TRUE.toString())
                .proxyMethod(Boolean.TRUE.toString())
                .proxyQueryParams(Boolean.TRUE.toString())
                .build();
    }
}
