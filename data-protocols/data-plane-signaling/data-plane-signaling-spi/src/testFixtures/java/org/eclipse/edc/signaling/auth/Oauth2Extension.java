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
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator;
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

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static jakarta.json.Json.createObjectBuilder;

public class Oauth2Extension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private final WireMockServer server = new WireMockServer();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        server.start();
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
                .willReturn(okJson("{\"access_token\":\"" + generateJwt(id) + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));

        return createObjectBuilder()
                .add("type", "oauth2_client_credentials")
                .add("tokenEndpoint", server.baseUrl() + urlPath)
                .add("clientId", id)
                .add("clientSecret", "test-secret")
                .build();
    }

    private String generateJwt(String subject) {
        try {
            var key = new OctetSequenceKeyGenerator(256).generate();
            var signer = new MACSigner(key);
            var claimsSet = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                    .build();
            var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
