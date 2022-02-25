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

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.transformer;

import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.CONTRACT_ID_CLAIM;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.DATA_ADDRESS_CLAIM;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.EXPIRATION_DATE_CLAIM;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.PROCESS_ID_CLAIM;

public class ProxyEndpointDataReferenceTransformer implements EndpointDataReferenceTransformer {

    private final TokenGenerationService tokenGenerationService;
    private final DataEncrypter dataEncrypter;
    private final String endpoint;
    private final TypeManager typeManager;

    public ProxyEndpointDataReferenceTransformer(TokenGenerationService tokenGenerationService, DataEncrypter dataEncrypter, String endpoint, TypeManager typeManager) {
        this.tokenGenerationService = tokenGenerationService;
        this.dataEncrypter = dataEncrypter;
        this.endpoint = endpoint;
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
                .address(endpoint)
                .authKey(edr.getAuthKey())
                .authCode(result.getContent().getToken())
                .contractId(edr.getContractId())
                .expirationEpochSeconds(edr.getExpirationEpochSeconds())
                .build());
    }

    private Map<String, Object> createClaims(EndpointDataReference edr) {
        var dataAddressStr = dataEncrypter.encrypt(typeManager.writeValueAsString(createDataAddress(edr)));
        return Map.of(EXPIRATION_DATE_CLAIM, Date.from(Instant.ofEpochSecond(edr.getExpirationEpochSeconds())),
                CONTRACT_ID_CLAIM, edr.getContractId(),
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
