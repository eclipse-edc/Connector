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

package org.eclipse.edc.iam.identitytrust.sts.defaults.service;

import org.eclipse.edc.iam.identitytrust.sts.embedded.EmbeddedSecureTokenService;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClient;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClientTokenAdditionalParams;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientTokenGeneratorService;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsTokenGenerationProvider;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;

import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

public class StsClientTokenGeneratorServiceImpl implements StsClientTokenGeneratorService {

    private static final Map<String, Function<StsClientTokenAdditionalParams, String>> CLAIM_MAPPERS = Map.of(
            PRESENTATION_TOKEN_CLAIM, StsClientTokenAdditionalParams::getAccessToken);

    private final long tokenExpiration;
    private final StsTokenGenerationProvider tokenGenerationProvider;
    private final Function<StsClient, String> keyFunction;
    private final Clock clock;

    public StsClientTokenGeneratorServiceImpl(StsTokenGenerationProvider tokenGenerationProvider, Function<StsClient, String> keyFunction, Clock clock, long tokenExpiration) {
        this.tokenGenerationProvider = tokenGenerationProvider;
        this.keyFunction = keyFunction;
        this.clock = clock;
        this.tokenExpiration = tokenExpiration;
    }

    @Override
    public ServiceResult<TokenRepresentation> tokenFor(StsClient client, StsClientTokenAdditionalParams additionalParams) {

        var embeddedTokenGenerator = new EmbeddedSecureTokenService(tokenGenerationProvider.tokenGeneratorFor(client), () -> keyFunction.apply(client), client::getPublicKeyReference,
                clock, tokenExpiration);

        var initialClaims = Map.of(
                ISSUER, client.getDid(),
                SUBJECT, client.getDid(),
                AUDIENCE, additionalParams.getAudience());

        var claims = CLAIM_MAPPERS.entrySet().stream()
                .filter(entry -> entry.getValue().apply(additionalParams) != null)
                .reduce(initialClaims, (accumulator, entity) ->
                        Optional.ofNullable(entity.getValue().apply(additionalParams))
                                .map(enrichClaimsWith(accumulator, entity.getKey()))
                                .orElse(accumulator), (a, b) -> b);

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

    private Function<String, Map<String, String>> enrichClaimsWith(Map<String, String> claims, String claim) {
        return (claimValue) -> {
            var newClaims = new HashMap<>(claims);
            newClaims.put(claim, claimValue);
            return Collections.unmodifiableMap(newClaims);
        };
    }

}
