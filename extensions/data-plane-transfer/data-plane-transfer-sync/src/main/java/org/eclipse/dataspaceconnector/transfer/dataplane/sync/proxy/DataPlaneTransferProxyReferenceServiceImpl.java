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

import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyReferenceService;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Date;
import java.util.HashMap;

import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;

public class DataPlaneTransferProxyReferenceServiceImpl implements DataPlaneTransferProxyReferenceService {

    private final TokenGenerationService tokenGenerationService;
    private final TypeManager typeManager;
    private final long tokenValiditySeconds;
    private final DataEncrypter dataEncrypter;
    private final Clock clock;

    public DataPlaneTransferProxyReferenceServiceImpl(TokenGenerationService tokenGenerationService, TypeManager typeManager, long tokenValiditySeconds, DataEncrypter dataEncrypter, Clock clock) {
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
        var decorator = new DataPlaneProxyTokenDecorator(Date.from(clock.instant().plusSeconds(tokenValiditySeconds)), request.getContractId(), encryptedDataAddress);
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
