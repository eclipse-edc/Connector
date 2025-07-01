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

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.Matchers.equalTo;
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
                    .body("isSystemHealthy", equalTo(true)));

        }
    }

    @Nested
    @EndToEndTest
    class ControlPlaneDcp extends SmokeTest {

        @RegisterExtension
        protected RuntimeExtension runtime = new RuntimePerMethodExtension(
                new EmbeddedRuntime("control-plane-dcp-bom", ":dist:bom:controlplane-dcp-bom")
                        .configurationProvider(() -> ConfigFactory.fromMap(new HashMap<>() {{
                            put("edc.iam.sts.oauth.token.url", "https://sts.com/token");
                            put("edc.iam.sts.oauth.client.id", "test-client");
                            put("edc.iam.sts.oauth.client.secret.alias", "test-alias");
                            put("web.http.port", DEFAULT_PORT);
                            put("web.http.path", DEFAULT_PATH);
                            put("web.http.version.port", String.valueOf(getFreePort()));
                            put("web.http.version.path", "/api/version");
                            put("web.http.control.port", String.valueOf(getFreePort()));
                            put("web.http.control.path", "/api/control");
                            put("web.http.management.port", "8081");
                            put("web.http.management.path", "/api/management");
                            put("edc.iam.sts.privatekey.alias", "privatekey");
                            put("edc.iam.sts.publickey.id", "publickey");
                            put("edc.iam.issuer.id", "did:web:someone");
                        }
                    }))
        );
    }

    @Nested
    @EndToEndTest
    public class DataPlaneBase extends SmokeTest {

        private static ClientAndServer server;
        @RegisterExtension
        protected RuntimeExtension runtime =
                new RuntimePerMethodExtension(new EmbeddedRuntime("data-plane-base-bom", ":dist:bom:dataplane-base-bom")
                        .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                                "edc.transfer.proxy.token.verifier.publickey.alias", "test-alias",
                                "edc.transfer.proxy.token.signer.privatekey.alias", "private-alias",
                                "edc.dpf.selector.url", "http://localhost:%s/selector".formatted(server.getPort()),
                                "web.http.control.port", "8081",
                                "web.http.control.path", "/api/control",
                                "web.http.version.port", String.valueOf(getFreePort()),
                                "web.http.version.path", "/api/version",
                                "web.http.port", DEFAULT_PORT,
                                "web.http.path", DEFAULT_PATH)))
                );

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

}
