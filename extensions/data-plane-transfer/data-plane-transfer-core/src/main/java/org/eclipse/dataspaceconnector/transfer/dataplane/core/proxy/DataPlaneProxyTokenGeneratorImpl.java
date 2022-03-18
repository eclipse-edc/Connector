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

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.common.token.JwtDecorator;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyTokenGenerator;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Date;

import static org.eclipse.dataspaceconnector.spi.types.domain.dataplane.DataPlaneConstants.CONTRACT_ID;
import static org.eclipse.dataspaceconnector.spi.types.domain.dataplane.DataPlaneConstants.DATA_ADDRESS;

public class DataPlaneProxyTokenGeneratorImpl implements DataPlaneProxyTokenGenerator {

    private final TypeManager typeManager;
    private final DataEncrypter dataEncrypter;
    private final TokenGenerationService tokenGenerationService;
    private final long tokenValiditySeconds;

    public DataPlaneProxyTokenGeneratorImpl(TypeManager typeManager,
                                            DataEncrypter dataEncrypter,
                                            TokenGenerationService tokenGenerationService,
                                            long tokenValiditySeconds) {
        this.typeManager = typeManager;
        this.dataEncrypter = dataEncrypter;
        this.tokenGenerationService = tokenGenerationService;
        this.tokenValiditySeconds = tokenValiditySeconds;
    }

    @Override
    public Result<TokenRepresentation> generate(@NotNull DataAddress dataAddress, @NotNull String contractId) {
        var expirationDate = Date.from(Instant.now().plusSeconds(tokenValiditySeconds));
        var dataAddressStr = dataEncrypter.encrypt(typeManager.writeValueAsString(dataAddress));
        var decorator = new JwtDecorator() {
            @Override
            public void decorate(JWSHeader.Builder header, JWTClaimsSet.Builder claimsSet) {
                claimsSet.expirationTime(expirationDate)
                        .claim(CONTRACT_ID, contractId)
                        .claim(DATA_ADDRESS, dataAddressStr)
                        .build();
            }
        };

        return tokenGenerationService.generate(decorator);
    }
}
