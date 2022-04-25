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

package org.eclipse.dataspaceconnector.transfer.dataplane.core.proxy;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreator;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_CODE;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_KEY;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.PROXY_BODY;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.PROXY_METHOD;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.PROXY_PATH;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.PROXY_QUERY_PARAMS;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.TYPE;

/**
 * Transforms {@link EndpointDataReference} returned by the provider Control Plane in such a way that
 * the consumer Data Plane becomes a Data Proxy to query data.
 * This implies that the data query should first hit the consumer Data Plane, which then forward the
 * call to the provider Data Plane, which finally reach the actual data source.
 */
public class DataPlaneTransferConsumerProxyTransformer implements EndpointDataReferenceTransformer {

    private final DataPlaneTransferProxyCreator proxyCreator;

    public DataPlaneTransferConsumerProxyTransformer(DataPlaneTransferProxyCreator proxyCreator) {
        this.proxyCreator = proxyCreator;
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
                .address(address)
                .contractId(contractId);
        edr.getProperties().forEach(builder::property);
        return proxyCreator.createProxy(builder.build());
    }

    private static DataAddress toHttpDataAddress(EndpointDataReference edr) {
        return DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ENDPOINT, edr.getEndpoint())
                .property(AUTHENTICATION_KEY, edr.getAuthKey())
                .property(AUTHENTICATION_CODE, edr.getAuthCode())
                .property(PROXY_QUERY_PARAMS, Boolean.TRUE.toString())
                .property(PROXY_PATH, Boolean.TRUE.toString())
                .property(PROXY_METHOD, Boolean.TRUE.toString())
                .property(PROXY_BODY, Boolean.TRUE.toString())
                .build();
    }
}
