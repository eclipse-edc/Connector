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

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.common.token.JwtDecorator;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.iam.oauth2.core.jwt.Fingerprint.sha1Base64Fingerprint;

public class DefaultJwtDecorator implements JwtDecorator {

    private final String audience;
    private final String clientId;
    private final byte[] encodedCertificate;
    private final long expiration;

    public DefaultJwtDecorator(String audience, String clientId, byte[] encodedCertificate, long expiration) {
        this.audience = audience;
        this.clientId = clientId;
        this.encodedCertificate = encodedCertificate;
        this.expiration = expiration;
    }

    @Override
    public void decorate(JWSHeader.Builder header, JWTClaimsSet.Builder claimsSet) {
        header.x509CertThumbprint(new Base64URL(sha1Base64Fingerprint(encodedCertificate)));
        claimsSet.audience(audience)
                .issuer(clientId)
                .subject(clientId)
                .jwtID(UUID.randomUUID().toString())
                .notBeforeTime(new Date())
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(expiration)));
    }
}
