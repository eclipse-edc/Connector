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

package org.eclipse.edc.iam.decentralizedclaims.spi.credentialservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.MultiValue;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.text.ParseException;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;

/**
 * JUnit extension that sets up a mock Credential Service.
 * Provides a CredentialService instance for use in tests.
 */
public class CredentialServiceEndToEndExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private final WireMockServer wireMockServer;
    private final LazySupplier<CredentialService> credentialService;

    public CredentialServiceEndToEndExtension() {
        credentialService = createCredentialService();
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort()
                .extensions(new DynamicDidResponse(credentialService),
                        new DynamicPresentationResponse(credentialService),
                        new DynamicStsResponse(credentialService)));
    }


    private LazySupplier<CredentialService> createCredentialService() {
        return new LazySupplier<>(() -> new CredentialService(wireMockServer.port()));
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        wireMockServer.stop();
    }

    public CredentialService getCredentialService() {
        return credentialService.get();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        wireMockServer.stubFor(post("/token")
                .willReturn(aResponse().withTransformers("sts-dynamic-response")));

        wireMockServer.stubFor(get(urlPathTemplate("/{participantContextId}/did.json"))
                .willReturn(aResponse().withTransformers("did-dynamic-response")));
        wireMockServer.stubFor(post(urlPathTemplate("/credentials/{participantContextId}/presentations/query"))
                .willReturn(aResponse().withTransformers("presentation-dynamic-response")));


        wireMockServer.start();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(CredentialService.class);
    }

    @Override
    public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().equals(CredentialService.class)) {
            return getCredentialService();
        }
        return null;
    }

    public Config dcpConfig(String participantContextId) {
        var did = credentialService.get().didFor(participantContextId);
        return ConfigFactory.fromMap(Map.of(
                "edc.participant.id", did,
                "edc.iam.issuer.id", did,
                "edc.iam.sts.oauth.client.id", participantContextId,
                "edc.iam.sts.oauth.client.secret.alias", participantContextId + "-alias",
                "edc.iam.sts.oauth.token.url", wireMockServer.baseUrl() + "/token"
        ));
    }

    public String didFor(String participantContextId) {
        return credentialService.get().didFor(participantContextId);
    }

    public static class DynamicDidResponse implements ResponseDefinitionTransformerV2 {

        private final LazySupplier<CredentialService> credentialService;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public DynamicDidResponse(LazySupplier<CredentialService> credentialService) {
            this.credentialService = credentialService;
        }

        @Override
        public ResponseDefinition transform(ServeEvent serveEvent) {

            var participantContextId = serveEvent.getRequest().getPathParameters().get("participantContextId");

            if (participantContextId == null) {
                return new ResponseDefinitionBuilder()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .build();
            }
            var didDocument = credentialService.get().getDidDocument(participantContextId);
            try {
                var body = objectMapper.writeValueAsString(didDocument);
                return new ResponseDefinitionBuilder()
                        .withStatus(200)
                        .withBody(body)
                        .withHeader("Content-Type", "application/json")
                        .build();
            } catch (JsonProcessingException e) {
                return new ResponseDefinitionBuilder()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .build();
            }


        }

        @Override
        public String getName() {
            return "did-dynamic-response";
        }

        @Override
        public boolean applyGlobally() {
            return false;
        }
    }

    public static class DynamicPresentationResponse implements ResponseDefinitionTransformerV2 {

        private final LazySupplier<CredentialService> credentialService;

        public DynamicPresentationResponse(LazySupplier<CredentialService> credentialService) {
            this.credentialService = credentialService;
        }

        @Override
        public ResponseDefinition transform(ServeEvent serveEvent) {

            var participantContextId = serveEvent.getRequest().getPathParameters().get("participantContextId");

            var token = serveEvent.getRequest().getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return new ResponseDefinitionBuilder()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .build();
            }

            try {
                var signedJwt = SignedJWT.parse(token.replace("Bearer ", ""));
                var audience = signedJwt.getJWTClaimsSet().getIssuer();
                var presentation = credentialService.get().createVpJwt(participantContextId, audience);

                var body = """
                        {
                          "@context": [
                            "https://w3id.org/dspace-dcp/v1.0/dcp.jsonld"
                          ],
                          "type": "PresentationResponseMessage",
                          "presentation": [
                            "%s"
                          ]
                        }
                        """.formatted(presentation);

                return new ResponseDefinitionBuilder()
                        .withStatus(200)
                        .withBody(body)
                        .withHeader("Content-Type", "application/json")
                        .build();
            } catch (ParseException e) {
                return new ResponseDefinitionBuilder()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .build();
            }

        }

        @Override
        public String getName() {
            return "presentation-dynamic-response";
        }

        @Override
        public boolean applyGlobally() {
            return false;
        }
    }

    public static class DynamicStsResponse implements ResponseDefinitionTransformerV2 {

        private final LazySupplier<CredentialService> credentialService;

        public DynamicStsResponse(LazySupplier<CredentialService> credentialService) {
            this.credentialService = credentialService;
        }

        @Override
        public ResponseDefinition transform(ServeEvent serveEvent) {

            var participantContextId = serveEvent.getRequest().getFormParameters().get("client_id");
            var audience = serveEvent.getRequest().getFormParameters().get("audience");
            var bearerAccessScope = serveEvent.getRequest().getFormParameters().get("bearer_access_scope");

            if (participantContextId == null || participantContextId.values().isEmpty()) {
                return new ResponseDefinitionBuilder()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .build();
            }

            if (audience == null || audience.values().isEmpty()) {
                return new ResponseDefinitionBuilder()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .build();
            }

            var scope = Optional.ofNullable(bearerAccessScope).map(MultiValue::firstValue).orElse(null);
            var token = credentialService.get().createStsToken(participantContextId.firstValue(), audience.firstValue(), scope, null);

            var body = format("""
                    {"access_token": "%s", "expires_in": 3600}""", token);

            return new ResponseDefinitionBuilder()
                    .withStatus(200)
                    .withBody(body)
                    .withHeader("Content-Type", "application/json")
                    .build();
        }

        @Override
        public String getName() {
            return "sts-dynamic-response";
        }

        @Override
        public boolean applyGlobally() {
            return false;
        }
    }

}
