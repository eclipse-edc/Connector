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

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.transfer.dataplane.spi.DataPlaneTransferConstants.CONTRACT_ID;

/**
 * Transforms {@link EndpointDataReference} returned by the provider Control Plane so that
 * the consumer Data Plane becomes a Data Proxy to query data.
 * This implies that the data query should first hit the consumer Data Plane, which then forward the
 * call to the provider Data Plane, which finally reach the actual data source.
 */
public class DataPlaneTransferConsumerProxyTransformer implements EndpointDataReferenceTransformer {

    private final DataPlaneTransferProxyResolver proxyResolver;
    private final DataPlaneTransferProxyReferenceService proxyReferenceCreator;

    public DataPlaneTransferConsumerProxyTransformer(DataPlaneTransferProxyResolver proxyResolver, DataPlaneTransferProxyReferenceService proxyCreator) {
        this.proxyResolver = proxyResolver;
        this.proxyReferenceCreator = proxyCreator;
    }

    @Override
    public boolean canHandle(@NotNull EndpointDataReference edr) {
        return true;
    }

    /**
     * Convert the consumer Data Plane insto a proxy for querying the provider Data Plane.
     *
     * @param edr provider {@link EndpointDataReference}
     * @return consumer {@link EndpointDataReference}
     */
    @Override
    public Result<EndpointDataReference> transform(@NotNull EndpointDataReference edr) {
        var address = toHttpDataAddress(edr);
        var contractId = edr.getProperties().get(CONTRACT_ID);
        if (contractId == null) {
            return Result.failure(format("Cannot transform endpoint data reference with id %s as contract id is missing", edr.getId()));
        }

        var proxyUrl = proxyResolver.resolveProxyUrl(address);
        if (proxyUrl.failed()) {
            return Result.failure(format("Failed to resolve proxy url for endpoint data reference %s\n %s", edr.getId(), String.join(",", proxyUrl.getFailureMessages())));
        }

        var builder = DataPlaneTransferProxyCreationRequest.Builder.newInstance()
                .id(edr.getId())
                .contentAddress(address)
                .proxyEndpoint(proxyUrl.getContent())
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
