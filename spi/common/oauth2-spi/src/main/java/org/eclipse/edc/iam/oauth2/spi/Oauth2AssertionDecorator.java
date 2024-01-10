/*
 *  Copyright (c) 2020 - 2022 Amadeus
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

package org.eclipse.edc.iam.oauth2.spi;

import org.eclipse.edc.token.spi.TokenDecorator;

import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUED_AT;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.JWT_ID;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;


public class Oauth2AssertionDecorator implements TokenDecorator {

    private final String audience;
    private final String clientId;
    private final Clock clock;
    private final long validity;

    public Oauth2AssertionDecorator(String audience, String clientId, Clock clock, long validity) {
        this.audience = audience;
        this.clientId = clientId;
        this.clock = clock;
        this.validity = validity;
    }

    @Override
    public void decorate(Map<String, Object> claims, Map<String, Object> headers) {
        claims.putAll(Map.of(
                AUDIENCE, List.of(audience),
                ISSUER, clientId,
                SUBJECT, clientId,
                JWT_ID, UUID.randomUUID().toString(),
                ISSUED_AT, Date.from(clock.instant()),
                EXPIRATION_TIME, Date.from(clock.instant().plusSeconds(validity))));
    }
}
