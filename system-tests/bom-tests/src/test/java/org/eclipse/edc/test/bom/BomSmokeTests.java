/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.test.bom;

import org.eclipse.edc.boot.vault.InMemoryVault;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.spi.security.Vault;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

public class BomSmokeTests {
    abstract static class SmokeTest {
        public static final String DEFAULT_PORT = "8080";
        public static final String DEFAULT_PATH = "/api";

        @Test
        void assertRuntimeReady() {
            await().untilAsserted(() -> given()
                    .baseUri("http://localhost:" + DEFAULT_PORT + DEFAULT_PATH + "/check/startup")
                    .get()
                    .then()
                    .statusCode(200)
                    .log().ifValidationFails()
                    .body("isSystemHealthy", Matchers.equalTo(true)));

        }
    }

    @Nested
    @EndToEndTest
    class ControlPlaneDcp extends SmokeTest {

        @RegisterExtension
        protected RuntimeExtension runtime =
                new RuntimePerMethodExtension(new EmbeddedRuntime("control-plane-dcp-bom",
                        Map.of(
                                "edc.iam.sts.oauth.token.url", "https://sts.com/token",
                                "edc.iam.sts.oauth.client.id", "test-client",
                                "edc.iam.sts.oauth.client.secret.alias", "test-alias",
                                "web.http.port", DEFAULT_PORT,
                                "web.http.path", DEFAULT_PATH,
                                "web.http.management.port", "8081",
                                "web.http.management.path", "/api/management"),
                        ":dist:bom:controlplane-dcp-bom"
                ));
    }

    @Nested
    @EndToEndTest
    class ControlPlaneOauth2 extends SmokeTest {

        @RegisterExtension
        protected static RuntimeExtension runtime =
                new RuntimePerMethodExtension(new EmbeddedRuntime("control-plane-oauth2-bom",
                        Map.of(
                                "edc.oauth.token.url", "https://oauth2.com/token",
                                "edc.oauth.certificate.alias", "test-alias",
                                "edc.oauth.private.key.alias", "private-test-alias",
                                "web.http.management.port", "8081",
                                "web.http.management.path", "/api/management",
                                "web.http.port", DEFAULT_PORT,
                                "web.http.path", DEFAULT_PATH,
                                "edc.oauth.provider.jwks.url", "http://localhost:9999/jwks",
                                "edc.oauth.client.id", "test-client"),
                        ":dist:bom:controlplane-oauth2-bom"
                ));
        private static ClientAndServer jwksServer;

        @BeforeAll
        static void setup() {
            var v = new InMemoryVault(mock());
            v.storeSecret("test-alias", getResourceFileContentAsString("cert.pem"));
            runtime.registerServiceMock(Vault.class, v);

            // mock the JWKS server, respond with some arbitrary JWKS
            jwksServer = ClientAndServer.startClientAndServer(9999);
            jwksServer.when(request().withPath("/jwks").withMethod("GET"))
                    .respond(response().withStatusCode(200).withBody(getResourceFileContentAsString("jwks_response.json")));
        }

        @AfterAll
        static void cleanup() {
            stopQuietly(jwksServer);
        }
    }

    @Nested
    @EndToEndTest
    public class DataPlaneBase extends SmokeTest {

        private static ClientAndServer server;
        @RegisterExtension
        protected RuntimeExtension runtime =
                new RuntimePerMethodExtension(new EmbeddedRuntime("data-plane-base-bom",
                        Map.of(
                                "edc.transfer.proxy.token.verifier.publickey.alias", "test-alias",
                                "edc.transfer.proxy.token.signer.privatekey.alias", "private-alias",
                                "edc.dpf.selector.url", "http://localhost:%s/selector".formatted(server.getPort()),
                                "web.http.control.port", "8081",
                                "web.http.control.path", "/api/control",
                                "web.http.port", DEFAULT_PORT,
                                "web.http.path", DEFAULT_PATH),
                        ":dist:bom:dataplane-base-bom"
                ));

        @BeforeAll
        static void setup() {
            server = ClientAndServer.startClientAndServer(getFreePort());
            server.when(request().withPath("/selector"))
                    .respond(response().withStatusCode(200));
        }

        @AfterAll
        static void afterAll() {
            stopQuietly(server);
        }
    }

    @Nested
    @EndToEndTest
    class StsFeature extends SmokeTest {

        @RegisterExtension
        protected RuntimeExtension runtime =
                new RuntimePerMethodExtension(new EmbeddedRuntime("data-plane-base-bom",
                        Map.of(
                                "web.http.port", DEFAULT_PORT,
                                "web.http.path", DEFAULT_PATH,
                                "edc.api.accounts.key", "password"),
                        ":dist:bom:sts-feature-bom"
                ));
    }
}
