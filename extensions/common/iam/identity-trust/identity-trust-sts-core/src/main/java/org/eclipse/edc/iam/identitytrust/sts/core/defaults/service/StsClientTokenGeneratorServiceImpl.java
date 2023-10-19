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
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.TokenRepresentation;

import java.time.Clock;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

public class StsClientTokenGeneratorServiceImpl implements StsClientTokenGeneratorService {

    private final Map<String, EmbeddedSecureTokenService> tokenGenerations;
    // TODO configurable?
    private final Integer capacity = 100;
    private final long tokenExpiration;
    private final StsTokenGenerationProvider tokenGenerationProvider;
    private final Clock clock;

    public StsClientTokenGeneratorServiceImpl(StsTokenGenerationProvider tokenGenerationProvider, Clock clock, long tokenExpiration) {
        this.tokenGenerationProvider = tokenGenerationProvider;
        this.clock = clock;
        this.tokenExpiration = tokenExpiration;
        this.tokenGenerations = Collections.synchronizedMap(new LinkedHashMap<>(capacity, .75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, EmbeddedSecureTokenService> eldest) {
                return size() > capacity;
            }
        });
    }

    @Override
    public ServiceResult<TokenRepresentation> tokenFor(StsClient client, StsClientTokenAdditionalParams additionalParams) {
        var embeddedTokenGenerator = tokenGenerations.computeIfAbsent(client.getId(), (id) -> new EmbeddedSecureTokenService(tokenGenerationProvider.tokenGeneratorFor(client), clock, tokenExpiration));

        var claims = Map.of(
                ISSUER, client.getId(),
                SUBJECT, client.getId(),
                AUDIENCE, additionalParams.getAudience(),
                "client_id", client.getClientId());

        var tokenResult = embeddedTokenGenerator.createToken(claims, additionalParams.getBearerAccessScope());

        if (tokenResult.failed()) {
            return ServiceResult.badRequest(tokenResult.getFailureDetail());
        }
        return ServiceResult.success(tokenResult.getContent());
    }

}
