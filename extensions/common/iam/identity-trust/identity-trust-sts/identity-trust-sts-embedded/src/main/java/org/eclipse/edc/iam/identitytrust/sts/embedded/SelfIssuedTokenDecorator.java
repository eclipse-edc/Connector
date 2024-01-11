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

package org.eclipse.edc.iam.identitytrust.sts.embedded;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.token.spi.TokenDecorator;

import java.time.Clock;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;

/**
 * Decorator for Self-Issued ID token and Access Token. It appends input claims and
 * generic claims like iat, exp, and jti
 */
class SelfIssuedTokenDecorator implements TokenDecorator {
    private final Map<String, String> claims;
    private final Clock clock;
    private final long validity;

    SelfIssuedTokenDecorator(Map<String, String> claims, Clock clock, long validity) {
        this.claims = claims;
        this.clock = clock;
        this.validity = validity;
    }

    @Override
    public TokenParameters.Builder decorate(TokenParameters.Builder tokenParameters) {
        this.claims.forEach(tokenParameters::additional);
        return tokenParameters.additional(ISSUED_AT, Date.from(clock.instant()))
                .additional(EXPIRATION_TIME, Date.from(clock.instant().plusSeconds(validity)))
                .additional(JWT_ID, UUID.randomUUID().toString());
    }
}
