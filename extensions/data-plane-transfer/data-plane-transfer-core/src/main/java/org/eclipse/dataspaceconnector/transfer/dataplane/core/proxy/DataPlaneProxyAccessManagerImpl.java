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

import org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyAccessManager;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.token.DataPlaneTransferTokenGenerator;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Date;

import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_CODE;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.AUTHENTICATION_KEY;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.TYPE;

/**
 * Uses the public API exposed by a Data Plane instance to proxy the access to the actual data.
 */
public class DataPlaneProxyAccessManagerImpl implements DataPlaneProxyAccessManager {

    private final String endpoint;
    private final DataPlaneTransferTokenGenerator tokenGenerator;
    private final TypeManager typeManager;
    private final DataEncrypter dataEncrypter;
    private final long tokenValiditySeconds;

    public DataPlaneProxyAccessManagerImpl(String endpoint, DataPlaneTransferTokenGenerator tokenGenerator, TypeManager typeManager, DataEncrypter dataEncrypter, long tokenValiditySeconds) {
        this.endpoint = endpoint;
        this.tokenGenerator = tokenGenerator;
        this.typeManager = typeManager;
        this.dataEncrypter = dataEncrypter;
        this.tokenValiditySeconds = tokenValiditySeconds;
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
        var tokenGenerationResult = createToken(request.getAddress(), request.getContractId());
        if (tokenGenerationResult.failed()) {
            return Result.failure(tokenGenerationResult.getFailureMessages());
        }

        var builder = EndpointDataReference.Builder.newInstance()
                .id(request.getId())
                .endpoint(endpoint)
                .authKey(DataPlaneConstants.PUBLIC_API_AUTH_HEADER)
                .authCode(tokenGenerationResult.getContent().getToken())
                .properties(request.getProperties());
        return Result.success(builder.build());
    }

    private Result<TokenRepresentation> createToken(DataAddress dataAddress, String contractId) {
        var expirationDate = Date.from(Instant.now().plusSeconds(tokenValiditySeconds));
        var encryptedDataAddress = dataEncrypter.encrypt(typeManager.writeValueAsString(dataAddress));
        return tokenGenerator.generate(new DataPlaneProxyTokenDecorator(expirationDate, contractId, encryptedDataAddress));
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
