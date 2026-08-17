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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * A stub OAuth2 Token Exchange broker (RFC 8693) used to exercise the {@code oauth2_token_exchange} Data Plane
 * Signaling authorization profile. It exposes a token exchange endpoint and a JWKS endpoint, and signs the minted
 * tokens with a generated EC key so that receiving parties perform real signature verification.
 */
public class TokenExchangeExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    public static final String PROFILE_TYPE = "oauth2_token_exchange";
    public static final String SCOPE = "signaling:dataflow";
    /**
     * The single audience this broker mints tokens for. The profile carries a broker-wide audience, not a
     * per-receiver one, which is why every principal registered here shares it.
     */
    public static final String AUDIENCE = "dps-signaling";

    private static final String TOKEN_PATH = "/token";
    private static final String JWKS_PATH = "/.well-known/jwks.json";
    private static final long TOKEN_VALIDITY_SECONDS = 3600;

    private final WireMockServer server = new WireMockServer(getFreePort());
    private ECKey signingKey;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        signingKey = new ECKeyGenerator(Curve.P_256).keyID("broker-key").keyUse(KeyUse.SIGNATURE).generate();
        server.start();
        server.stubFor(get(urlPathEqualTo(JWKS_PATH))
                .willReturn(okJson(new JWKSet(signingKey.toPublicJWK()).toString())));
    }

    @Override
    public void afterAll(ExtensionContext context) {
        server.stop();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getParameterizedType().equals(TokenExchangeExtension.class);
    }

    @Override
    public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return this;
    }

    /**
     * Stubs an exchange for the given component and returns the {@code authorization} object that names this broker,
     * with the {@code audience} carried by the profile.
     * <p>
     * The minted token is shaped as the specification prescribes: {@code sub} is an absolute Principal Resource URI
     * derived from the component id, while the component id itself is carried by the {@code client_id} claim. The two
     * therefore differ, which is what makes the receiving party's {@code client_id} precedence observable.
     *
     * @param componentId the data plane / control plane the exchanged token speaks for, becomes the {@code client_id} claim
     */
    public JsonObject registerPrincipal(String componentId) {
        return registerPrincipal(componentId, true);
    }

    /**
     * Like {@link #registerPrincipal(String)}, but the returned profile omits the {@code audience} property, so that
     * the issuing party has to fall back to its configured default audience. The stubbed exchange still only matches
     * requests carrying {@link #AUDIENCE}.
     */
    public JsonObject registerPrincipalWithoutAudience(String componentId) {
        return registerPrincipal(componentId, false);
    }

    private JsonObject registerPrincipal(String componentId, boolean audienceOnProfile) {
        var principal = principalResourceId(componentId);
        var token = mintToken(principal, componentId);

        server.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .withRequestBody(containing("resource=" + urlEncode(principal)))
                .withRequestBody(containing("audience=" + urlEncode(AUDIENCE)))
                .willReturn(okJson("""
                        {
                          "access_token": "%s",
                          "issued_token_type": "urn:ietf:params:oauth:token-type:jwt",
                          "token_type": "Bearer",
                          "expires_in": %d,
                          "scope": "%s"
                        }
                        """.formatted(token, TOKEN_VALIDITY_SECONDS, SCOPE))));

        var profile = createObjectBuilder()
                .add("type", PROFILE_TYPE)
                .add("tokenExchangeEndpoint", tokenExchangeEndpoint())
                .add("issuer", issuer())
                .add("jwksUri", jwksUri())
                .add("resource", principal)
                .add("scope", SCOPE);

        if (audienceOnProfile) {
            profile.add("audience", AUDIENCE);
        }

        return profile.build();
    }

    public String issuer() {
        return server.baseUrl();
    }

    public String tokenExchangeEndpoint() {
        return server.baseUrl() + TOKEN_PATH;
    }

    public String jwksUri() {
        return server.baseUrl() + JWKS_PATH;
    }

    /**
     * The number of token exchanges the broker has served for the given component.
     */
    public int exchangeCount(String componentId) {
        return server.findAll(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                .withRequestBody(containing("resource=" + urlEncode(principalResourceId(componentId))))
                .withRequestBody(containing("audience=" + urlEncode(AUDIENCE)))).size();
    }

    /**
     * The Principal Resource URI this broker names the given component with.
     */
    public String principalResourceId(String componentId) {
        return "urn:dps:principal:" + componentId;
    }

    /**
     * The number of times the broker's JWKS endpoint has been fetched, i.e. how often a receiving party verified a
     * token signature.
     */
    public int jwksRequestCount() {
        return server.findAll(getRequestedFor(urlPathEqualTo(JWKS_PATH))).size();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String mintToken(String principal, String componentId) {
        try {
            var now = Instant.now();
            var claims = new JWTClaimsSet.Builder()
                    .issuer(issuer())
                    .subject(principal)
                    .audience(List.of(AUDIENCE))
                    .claim("client_id", componentId)
                    .claim("scope", SCOPE)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(TOKEN_VALIDITY_SECONDS)))
                    // deliberately no 'jti': the stub returns the same token for every exchange, and a repeated jti
                    // would be rejected as a replay by the receiving party. A real broker mints a fresh token per exchange.
                    .build();
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()).build(), claims);
            jwt.sign(new ECDSASigner(signingKey));
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
