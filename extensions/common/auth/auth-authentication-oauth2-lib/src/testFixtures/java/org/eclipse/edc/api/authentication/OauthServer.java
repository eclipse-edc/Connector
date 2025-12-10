/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.api.authentication;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.nimbusds.jose.JWSAlgorithm.ES256;

public class OauthServer {

    private final ECKey oauthServerSigningKey;
    private final String issuer;
    private final String scopes;

    public OauthServer(ECKey oauthServerSigningKey, String issuer, String scopes) {
        this.oauthServerSigningKey = oauthServerSigningKey;
        this.issuer = issuer;
        this.scopes = scopes;
    }

    public String createToken(String participantContextId) {
        return createToken(participantContextId, Map.of());
    }

    public String createToken(String participantContextId, String role) {
        return createToken(participantContextId, Map.of(), scopes, role);
    }

    public String createToken(String participantContextId, String scopes, String role) {
        return createToken(participantContextId, Map.of(), scopes, role);
    }

    public String createToken(String participantContextId, Map<String, String> additionalClaims) {
        return createToken(participantContextId, additionalClaims, scopes, ParticipantPrincipal.ROLE_PARTICIPANT);
    }

    public String createToken(String participantContextId, Map<String, String> additionalClaims, String scopes, String role) {
        var defaultClaims = new HashMap<String, Object>(Map.of(
                "sub", "test-subject",
                "iss", issuer,
                "iat", Instant.now().getEpochSecond(),
                "exp", Instant.now().plusSeconds(3600).getEpochSecond(),
                "jti", UUID.randomUUID().toString(),
                "scope", scopes,
                "role", role
        ));
        if (participantContextId != null) {
            defaultClaims.put("participant_context_id", participantContextId);
        }
        defaultClaims.putAll(additionalClaims);
        return createToken(defaultClaims);
    }

    public String createToken(Map<String, Object> claims) {
        try {
            var claimsBuilder = new JWTClaimsSet.Builder();
            claims.forEach(claimsBuilder::claim);
            var hdr = new JWSHeader.Builder(ES256).keyID(oauthServerSigningKey.getKeyID()).build();
            var jwt = new SignedJWT(hdr, claimsBuilder.build());
            var signer = new ECDSASigner(oauthServerSigningKey);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String createAdminToken() {
        return createToken(null, ParticipantPrincipal.ROLE_ADMIN);
    }

    public String createProvisionerToken() {
        return createToken(null, ParticipantPrincipal.ROLE_PROVISIONER);

    }
}
