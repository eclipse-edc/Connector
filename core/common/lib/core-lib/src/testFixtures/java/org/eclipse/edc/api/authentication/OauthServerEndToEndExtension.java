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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;

/**
 * JUnit extension that sets up a mock OAuth2 authorization server with a JWKS endpoint.
 * Provides an AuthServer instance for use in tests.
 */
public class OauthServerEndToEndExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private WireMockServer wireMockServer;
    private OauthServer authServer;
    private ECKey key;
    private String issuer;
    private String signingKeyId;
    private String scopes = "management-api:read management-api:write";

    private OauthServerEndToEndExtension() {
    }

    private ECKey generateEcKey(String signingKeyId) {
        try {
            return new ECKeyGenerator(Curve.P_256).keyID(signingKeyId)
                    .generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        wireMockServer.stop();
    }

    public OauthServer getAuthServer() {
        return authServer;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // create JWKS with the participant's key
        var jwks = createObjectBuilder()
                .add("keys", createArrayBuilder().add(createObjectBuilder(
                        key.toPublicJWK().toJSONObject())))
                .build()
                .toString();

        // use wiremock to host a JWKS endpoint
        wireMockServer.stubFor(any(urlPathEqualTo("/.well-known/jwks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jwks)));
        wireMockServer.start();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(OauthServer.class);
    }

    @Override
    public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().equals(OauthServer.class)) {
            return authServer;
        }
        return null;
    }

    public String getJwksUrl() {
        return wireMockServer.baseUrl() + "/.well-known/jwks";
    }

    public Config getConfig() {
        return ConfigFactory.fromMap(Map.of(
                "edc.iam.oauth2.issuer", issuer,
                "edc.iam.oauth2.jwks.url", getJwksUrl()));
    }

    public static class Builder {

        private final OauthServerEndToEndExtension ext;

        private Builder() {
            ext = new OauthServerEndToEndExtension();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder issuer(String issuer) {
            ext.issuer = issuer;
            return this;
        }

        public Builder signingKeyId(String signingKeyId) {
            ext.signingKeyId = signingKeyId;
            return this;
        }

        public Builder key(ECKey key) {
            ext.key = key;
            return this;
        }

        public Builder wireMockServer(WireMockServer server) {
            ext.wireMockServer = server;
            return this;
        }

        public Builder scopes(String scopes) {
            ext.scopes = scopes;
            return this;
        }

        public OauthServerEndToEndExtension build() {
            ext.wireMockServer = Objects.requireNonNullElseGet(ext.wireMockServer, () -> new WireMockServer(wireMockConfig().dynamicPort()));
            ext.issuer = Objects.requireNonNullElseGet(ext.issuer, () -> "test-issuer");
            ext.signingKeyId = Objects.requireNonNullElseGet(ext.signingKeyId, () -> UUID.randomUUID().toString());
            ext.key = Objects.requireNonNullElseGet(ext.key, () -> ext.generateEcKey(ext.signingKeyId));
            ext.authServer = new OauthServer(ext.key, ext.issuer, ext.scopes);
            return ext;
        }
    }
}
