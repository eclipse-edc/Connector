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
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyManager;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyTokenGenerator;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_CODE;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_KEY;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.TYPE;

/**
 * Uses the public API exposed by a Data Plane instance to proxy the access to the actual data.
 */
public class DataPlaneProxyManagerImpl implements DataPlaneProxyManager {

    private static final String DATA_PLANE_PUBLIC_API_AUTH_HEADER = "Authorization";

    private final String endpoint;
    private final DataPlaneProxyTokenGenerator tokenGenerator;

    public DataPlaneProxyManagerImpl(String endpoint, DataPlaneProxyTokenGenerator tokenGenerator) {
        this.endpoint = endpoint;
        this.tokenGenerator = tokenGenerator;
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

        var builder = DataPlaneProxyCreationRequest.Builder.newInstance()
                .id(edr.getId())
                .address(address)
                .contractId(contractId);
        edr.getProperties().forEach(builder::property);
        return createProxy(builder.build());
    }


    @Override
    public Result<EndpointDataReference> createProxy(@NotNull DataPlaneProxyCreationRequest request) {
        var tokenGenerationResult = tokenGenerator.generate(request.getAddress(), request.getContractId());
        if (tokenGenerationResult.failed()) {
            return Result.failure(tokenGenerationResult.getFailureMessages());
        }

        var builder = EndpointDataReference.Builder.newInstance()
                .id(request.getId())
                .endpoint(endpoint)
                .authKey(DATA_PLANE_PUBLIC_API_AUTH_HEADER)
                .authCode(tokenGenerationResult.getContent().getToken())
                .properties(request.getProperties());
        return Result.success(builder.build());
    }

    private static DataAddress toHttpDataAddress(EndpointDataReference edr) {
        return DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ENDPOINT, edr.getEndpoint())
                .property(AUTHENTICATION_KEY, edr.getAuthKey())
                .property(AUTHENTICATION_CODE, edr.getAuthCode())
                .build();
    }
}
