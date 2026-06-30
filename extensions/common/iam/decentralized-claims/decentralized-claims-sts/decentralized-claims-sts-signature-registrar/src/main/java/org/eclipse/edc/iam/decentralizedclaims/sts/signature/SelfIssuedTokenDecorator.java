/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.sts.signature;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.token.spi.TokenDecorator;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;

/**
 * Decorator for the Self-Issued ID token and the nested access token. It appends the input claims plus the generic
 * {@code iat}, {@code exp} and {@code jti} claims.
 */
class SelfIssuedTokenDecorator implements TokenDecorator {

    private final Map<String, Object> claims;
    private final Clock clock;
    private final long validity;

    SelfIssuedTokenDecorator(Map<String, Object> claims, Clock clock, long validity) {
        this.claims = claims;
        this.clock = clock;
        this.validity = validity;
    }

    @Override
    public TokenParameters.Builder decorate(TokenParameters.Builder tokenParameters) {
        var now = clock.instant();
        return tokenParameters
                .claims(claims)
                .claims(ISSUED_AT, now.getEpochSecond())
                .claims(EXPIRATION_TIME, now.plusSeconds(validity).getEpochSecond())
                .claims(JWT_ID, UUID.randomUUID().toString());
    }
}
