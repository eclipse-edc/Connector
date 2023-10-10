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

import org.eclipse.edc.jwt.spi.JwtDecorator;

import java.time.Clock;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;

public class SelfIssuedTokenDecorator implements JwtDecorator {
    private final Map<String, String> claims;
    private final Clock clock;
    private final long validity;

    public SelfIssuedTokenDecorator(Map<String, String> claims, Clock clock, long validity) {
        this.claims = claims;
        this.clock = clock;
        this.validity = validity;
    }

    @Override
    public Map<String, Object> claims() {
        var claims = new HashMap<String, Object>(this.claims);
        claims.put(ISSUED_AT, Date.from(clock.instant()));
        claims.put(EXPIRATION_TIME, Date.from(clock.instant().plusSeconds(validity)));
        claims.put(JWT_ID, UUID.randomUUID().toString());
        return claims;
    }

    @Override
    public Map<String, Object> headers() {
        return emptyMap();
    }
}
