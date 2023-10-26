/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.sts.core.defaults.service;

import org.eclipse.edc.iam.identitytrust.sts.embedded.EmbeddedSecureTokenService;
import org.eclipse.edc.iam.identitytrust.sts.model.StsClient;
import org.eclipse.edc.iam.identitytrust.sts.model.StsClientTokenAdditionalParams;
import org.eclipse.edc.iam.identitytrust.sts.service.StsClientTokenGeneratorService;
import org.eclipse.edc.iam.identitytrust.sts.service.StsTokenGenerationProvider;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;

import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.CLIENT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

public class StsClientTokenGeneratorServiceImpl implements StsClientTokenGeneratorService {

    public static final String ACCESS_TOKEN_CLAIM = "access_token";
    
    private final long tokenExpiration;
    private final StsTokenGenerationProvider tokenGenerationProvider;
    private final Clock clock;

    public StsClientTokenGeneratorServiceImpl(StsTokenGenerationProvider tokenGenerationProvider, Clock clock, long tokenExpiration) {
        this.tokenGenerationProvider = tokenGenerationProvider;
        this.clock = clock;
        this.tokenExpiration = tokenExpiration;

    }

    @Override
    public ServiceResult<TokenRepresentation> tokenFor(StsClient client, StsClientTokenAdditionalParams additionalParams) {
        var embeddedTokenGenerator = new EmbeddedSecureTokenService(tokenGenerationProvider.tokenGeneratorFor(client), clock, tokenExpiration);

        var initialClaims = Map.of(
                ISSUER, client.getId(),
                SUBJECT, client.getId(),
                AUDIENCE, additionalParams.getAudience(),
                CLIENT_ID, client.getClientId());

        var claims = Optional.ofNullable(additionalParams.getAccessToken())
                .map(enrichClaims(initialClaims))
                .orElse(initialClaims);

        var tokenResult = embeddedTokenGenerator.createToken(claims, additionalParams.getBearerAccessScope())
                .map(this::enrichWithExpiration);

        if (tokenResult.failed()) {
            return ServiceResult.badRequest(tokenResult.getFailureDetail());
        }
        return ServiceResult.success(tokenResult.getContent());
    }

    private TokenRepresentation enrichWithExpiration(TokenRepresentation tokenRepresentation) {
        return TokenRepresentation.Builder.newInstance()
                .token(tokenRepresentation.getToken())
                .additional(tokenRepresentation.getAdditional())
                .expiresIn(tokenExpiration)
                .build();
    }

    private Function<String, Map<String, String>> enrichClaims(Map<String, String> claims) {
        return (token) -> {
            var newClaims = new HashMap<>(claims);
            newClaims.put(ACCESS_TOKEN_CLAIM, token);
            return Collections.unmodifiableMap(newClaims);
        };
    }

}
