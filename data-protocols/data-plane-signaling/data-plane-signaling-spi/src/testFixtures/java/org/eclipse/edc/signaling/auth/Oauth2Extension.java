/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.auth;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.json.JsonObject;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.time.Instant;
import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * A stub OAuth2 authorization server used to exercise the {@code oauth2_client_credentials} Data Plane Signaling
 * authorization profile. It exposes a token endpoint per registered client and a JWKS endpoint, and signs the minted
 * tokens with a generated EC key so that receiving parties perform real signature verification.
 */
public class Oauth2Extension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final String JWKS_PATH = "/.well-known/jwks.json";
    private static final long TOKEN_VALIDITY_SECONDS = 3600;

    private final WireMockServer server = new WireMockServer(getFreePort());
    private ECKey signingKey;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        signingKey = new ECKeyGenerator(Curve.P_256).keyID("oauth2-key").keyUse(KeyUse.SIGNATURE).generate();
        server.start();
        server.stubFor(get(urlPathEqualTo(JWKS_PATH))
                .willReturn(okJson(new JWKSet(signingKey.toPublicJWK()).toString())));
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        server.stop();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getParameterizedType().equals(Oauth2Extension.class);
    }

    @Override
    public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return this;
    }

    /**
     * Register client and return the Authorization Profile with type oauth2_client_credentials
     *
     * @param id the component id.
     * @return the authorization profile.
     */
    public JsonObject registerClient(String id) {
        var urlPath = "/" + id + "/token";
        server.stubFor(post(urlPathEqualTo(urlPath))
                .willReturn(okJson("{\"access_token\":\"" + generateJwt(id) + "\",\"token_type\":\"Bearer\",\"expires_in\":" + TOKEN_VALIDITY_SECONDS + "}")));

        return createObjectBuilder()
                .add("type", "oauth2_client_credentials")
                .add("tokenEndpoint", server.baseUrl() + urlPath)
                .add("jwksUri", jwksUri())
                .add("clientId", id)
                .add("clientSecret", "test-secret")
                .build();
    }

    /**
     * The URI of the JWKS endpoint serving the public key the minted tokens are signed with.
     */
    public String jwksUri() {
        return server.baseUrl() + JWKS_PATH;
    }

    /**
     * The number of times the JWKS endpoint has been fetched, i.e. how often a receiving party verified a
     * token signature.
     */
    public int jwksRequestCount() {
        return server.findAll(getRequestedFor(urlPathEqualTo(JWKS_PATH))).size();
    }

    private String generateJwt(String subject) {
        try {
            var now = Instant.now();
            var claimsSet = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(TOKEN_VALIDITY_SECONDS)))
                    // deliberately no 'jti': the stub returns the same token for every request, and a repeated jti
                    // would be rejected as a replay by the receiving party. A real server mints a fresh token per request.
                    .build();
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()).build(), claimsSet);
            jwt.sign(new ECDSASigner(signingKey));
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
