/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.NOT_BEFORE;

/**
 * Opinionated decorator that adds "jti", "iat", "nbf" and "exp" claims to an existing set of claims, overwriting if the aforementioned
 * are already contained in the map.
 */
public class AccessTokenDecorator implements TokenDecorator {

    private final String jti;
    private final Instant now;
    private final Instant expiration;
    private final Map<String, String> claims;

    public AccessTokenDecorator(String jti, Instant now, Instant expiration, Map<String, String> claims) {
        this.jti = jti;
        this.now = now;
        this.expiration = expiration;
        this.claims = claims;
    }

    @Override
    public TokenParameters.Builder decorate(TokenParameters.Builder tokenParameters) {
        this.claims.forEach(tokenParameters::claims);
        return tokenParameters
                .claims(ISSUED_AT, Date.from(now))
                .claims(NOT_BEFORE, Date.from(now))
                .claims(EXPIRATION_TIME, Date.from(expiration))
                .claims(JWT_ID, jti);
    }
}
