/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.core.edr;

import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Map;

import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.CONTRACT_ID_CLAM;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.DATA_ADDRESS_CLAIM;

public class ProxyEndpointDataReferenceTransformer implements EndpointDataReferenceTransformer {

    private final TokenGenerationService tokenGenerationService;
    private final String dataPlanePublicEndpoint;
    private final TypeManager typeManager;

    public ProxyEndpointDataReferenceTransformer(@NotNull TokenGenerationService tokenGenerationService, @NotNull String dataPlanePublicEndpoint, @NotNull TypeManager typeManager) {
        this.tokenGenerationService = tokenGenerationService;
        this.dataPlanePublicEndpoint = dataPlanePublicEndpoint;
        this.typeManager = typeManager;

    }

    @Override
    public Result<EndpointDataReference> execute(@NotNull EndpointDataReference edr) {
        var result = tokenGenerationService.generate(createClaims(edr));
        if (result.failed()) {
            return Result.failure(result.getFailureMessages());
        }
        return Result.success(EndpointDataReference.Builder.newInstance()
                .correlationId(edr.getCorrelationId())
                .address(dataPlanePublicEndpoint)
                .authKey(edr.getAuthKey())
                .authCode(result.getContent().getToken())
                .contractId(edr.getContractId())
                .expirationEpochSeconds(edr.getExpirationEpochSeconds())
                .build());
    }

    private Map<String, Object> createClaims(EndpointDataReference edr) {
        var dataAddressStr = typeManager.writeValueAsString(createDataAddress(edr)); // TODO: encrypt data address
        return Map.of("exp", new Date(edr.getExpirationEpochSeconds()),
                CONTRACT_ID_CLAM, edr.getContractId(),
                DATA_ADDRESS_CLAIM, dataAddressStr);
    }

    private static DataAddress createDataAddress(EndpointDataReference edr) {
        // TODO: how to use the constant from HttpDataSchema here?
        return DataAddress.Builder.newInstance()
                .type("HttpData")
                .property("endpoint", edr.getAddress())
                .property("authKey", edr.getAuthKey())
                .property("authCode", edr.getAuthCode())
                .build();
    }
}
