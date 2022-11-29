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

package org.eclipse.edc.connector.dataplane.transfer.proxy;

import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.edc.connector.dataplane.transfer.spi.proxy.DataPlaneTransferProxyCreationRequest;
import org.eclipse.edc.connector.dataplane.transfer.spi.proxy.DataProxyReferenceService;
import org.eclipse.edc.connector.dataplane.transfer.spi.security.DataEncrypter;
import org.eclipse.edc.jwt.spi.TokenGenerationService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Date;
import java.util.HashMap;

import static org.eclipse.edc.connector.dataplane.transfer.spi.DataPlaneTransferConstants.CONTRACT_ID;

public class DataProxyReferenceServiceImpl implements DataProxyReferenceService {

    private final TokenGenerationService tokenGenerationService;
    private final TypeManager typeManager;
    private final long tokenValiditySeconds;
    private final DataEncrypter dataEncrypter;
    private final Clock clock;

    public DataProxyReferenceServiceImpl(TokenGenerationService tokenGenerationService, TypeManager typeManager, long tokenValiditySeconds, DataEncrypter dataEncrypter, Clock clock) {
        this.tokenGenerationService = tokenGenerationService;
        this.typeManager = typeManager;
        this.tokenValiditySeconds = tokenValiditySeconds;
        this.dataEncrypter = dataEncrypter;
        this.clock = clock;
    }

    /**
     * Creates an {@link EndpointDataReference} targeting public API of the provided Data Plane so that it is used
     * as a proxy to query the data from the data source.
     */
    @Override
    public Result<EndpointDataReference> createProxyReference(@NotNull DataPlaneTransferProxyCreationRequest request) {
        var encryptedDataAddress = dataEncrypter.encrypt(typeManager.writeValueAsString(request.getContentAddress()));
        var decorator = new DataProxyTokenDecorator(Date.from(clock.instant().plusSeconds(tokenValiditySeconds)), request.getContractId(), encryptedDataAddress);
        var tokenGenerationResult = tokenGenerationService.generate(decorator);
        if (tokenGenerationResult.failed()) {
            return Result.failure(tokenGenerationResult.getFailureMessages());
        }

        var props = new HashMap<>(request.getProperties());
        props.put(CONTRACT_ID, request.getContractId());

        var builder = EndpointDataReference.Builder.newInstance()
                .id(request.getId())
                .endpoint(request.getProxyEndpoint())
                .authKey(HttpHeaders.AUTHORIZATION)
                .authCode(tokenGenerationResult.getContent().getToken())
                .properties(props);
        return Result.success(builder.build());
    }
}
