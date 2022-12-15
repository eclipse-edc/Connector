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

import org.eclipse.edc.jwt.spi.JwtDecorator;

import java.time.Clock;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

public class Oauth2AssertionDecorator implements JwtDecorator {

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
    public Map<String, Object> headers() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> claims() {
        return Map.of(
                AUDIENCE, List.of(audience),
                ISSUER, clientId,
                SUBJECT, clientId,
                JWT_ID, UUID.randomUUID().toString(),
                ISSUED_AT, Date.from(clock.instant()),
                EXPIRATION_TIME, Date.from(clock.instant().plusSeconds(validity))
        );
    }
}
