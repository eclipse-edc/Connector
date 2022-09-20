/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.iam.oauth2.core.jwt;

import org.eclipse.dataspaceconnector.spi.jwt.JwtDecorator;

import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.iam.oauth2.core.jwt.Fingerprint.sha1Base64Fingerprint;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.AUDIENCE;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.EXPIRATION_TIME;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.ISSUED_AT;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.ISSUER;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.JWT_ID;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.SUBJECT;

public class DefaultJwtDecorator implements JwtDecorator {

    private final String audience;
    private final String clientId;
    private final byte[] encodedCertificate;
    private final Clock clock;
    private final long expiration;

    public DefaultJwtDecorator(String audience, String clientId, byte[] encodedCertificate, Clock clock, long expiration) {
        this.audience = audience;
        this.clientId = clientId;
        this.encodedCertificate = encodedCertificate;
        this.clock = clock;
        this.expiration = expiration;
    }

    @Override
    public Map<String, Object> headers() {
        return Map.of("x5t", sha1Base64Fingerprint(encodedCertificate));
    }

    @Override
    public Map<String, Object> claims() {
        return Map.of(
                AUDIENCE, List.of(audience),
                ISSUER, clientId,
                SUBJECT, clientId,
                JWT_ID, UUID.randomUUID().toString(),
                ISSUED_AT, Date.from(clock.instant()),
                EXPIRATION_TIME, Date.from(clock.instant().plusSeconds(expiration))
        );
    }
}
