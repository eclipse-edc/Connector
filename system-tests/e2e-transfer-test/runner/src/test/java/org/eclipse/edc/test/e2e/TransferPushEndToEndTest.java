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

package org.eclipse.edc.test.e2e;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.EdcClassRuntimesExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.createDatabase;
import static org.eclipse.edc.test.e2e.Runtimes.backendService;
import static org.eclipse.edc.test.e2e.Runtimes.controlPlane;
import static org.eclipse.edc.test.e2e.Runtimes.dataPlane;
import static org.eclipse.edc.test.e2e.Runtimes.postgresControlPlane;
import static org.eclipse.edc.test.e2e.Runtimes.postgresDataPlane;
import static org.eclipse.edc.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class TransferPushEndToEndTest {

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static final EdcClassRuntimesExtension RUNTIMES = new EdcClassRuntimesExtension(
                controlPlane("consumer-control-plane", CONSUMER.controlPlaneConfiguration()),
                backendService("consumer-backend-service", Map.of("web.http.port", String.valueOf(CONSUMER.backendService().getPort()))),
                dataPlane("provider-data-plane", PROVIDER.dataPlaneConfiguration()),
                controlPlane("provider-control-plane", PROVIDER.controlPlaneConfiguration()),
                backendService("provider-backend-service", Map.of("web.http.port", String.valueOf(PROVIDER.backendService().getPort())))
        );

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        static final BeforeAllCallback CREATE_DATABASES = context -> {
            createDatabase(CONSUMER.getName());
            createDatabase(PROVIDER.getName());
        };

        @RegisterExtension
        static final EdcClassRuntimesExtension RUNTIMES = new EdcClassRuntimesExtension(
                postgresControlPlane("consumer-control-plane", CONSUMER.controlPlanePostgresConfiguration()),
                backendService("consumer-backend-service", Map.of("web.http.port", String.valueOf(CONSUMER.backendService().getPort()))),
                postgresDataPlane("provider-data-plane", PROVIDER.dataPlanePostgresConfiguration()),
                postgresControlPlane("provider-control-plane", PROVIDER.controlPlanePostgresConfiguration()),
                backendService("provider-backend-service", Map.of("web.http.port", String.valueOf(PROVIDER.backendService().getPort())))
        );
    }

    abstract static class Tests extends TransferEndToEndTestBase {

        private final String assetId = UUID.randomUUID().toString();

        @BeforeEach
        void setUp() {
            PROVIDER.registerDataPlane();
        }

        @Test
        void httpToHttp() {
            var url = PROVIDER.backendService() + "/api/provider/data";
            Map<String, Object> dataAddressProperties = Map.of("type", "HttpData", "baseUrl", url);
            createResourcesOnProvider(assetId, noConstraintPolicy(), dataAddressProperties);
            var destination = httpDataAddress(CONSUMER.backendService() + "/api/consumer/store");

            var transferProcessId = CONSUMER.requestAsset(PROVIDER, assetId, noPrivateProperty(), destination);
            await().atMost(timeout).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(COMPLETED.name());

                given()
                        .baseUri(CONSUMER.backendService().toString())
                        .when()
                        .get("/api/consumer/data")
                        .then()
                        .statusCode(anyOf(is(200), is(204)))
                        .body(is(notNullValue()));
            });
        }

        @Test
        void httpToHttp_withTransferType() {
            var url = PROVIDER.backendService() + "/api/provider/data";
            Map<String, Object> dataAddressProperties = Map.of("type", "HttpData", "baseUrl", url);
            createResourcesOnProvider(assetId, noConstraintPolicy(), dataAddressProperties);
            var destination = httpDataAddress(CONSUMER.backendService() + "/api/consumer/store");

            var transferProcessId = CONSUMER.requestAsset(PROVIDER, assetId, noPrivateProperty(), destination, "HttpData-PUSH");
            await().atMost(timeout).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(COMPLETED.name());

                given()
                        .baseUri(CONSUMER.backendService().toString())
                        .when()
                        .get("/api/consumer/data")
                        .then()
                        .statusCode(anyOf(is(200), is(204)))
                        .body(is(notNullValue()));
            });
        }

        @Test
        @DisplayName("Provider pushes data to Consumer, Provider needs to authenticate the data request through an oauth2 server")
        void httpToHttp_oauth2Provisioning() {
            var sourceDataAddressProperties = Map.<String, Object>of(
                    "type", "HttpData",
                    "baseUrl", PROVIDER.backendService() + "/api/provider/oauth2data",
                    "oauth2:clientId", "clientId",
                    "oauth2:clientSecretKey", "provision-oauth-secret",
                    "oauth2:tokenUrl", PROVIDER.backendService() + "/api/oauth2/token"
            );

            createResourcesOnProvider(assetId, noConstraintPolicy(), sourceDataAddressProperties);
            var destination = httpDataAddress(CONSUMER.backendService() + "/api/consumer/store");

            var transferProcessId = CONSUMER.requestAsset(PROVIDER, assetId, noPrivateProperty(), destination);

            await().atMost(timeout).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(COMPLETED.name());

                given()
                        .baseUri(CONSUMER.backendService().toString())
                        .when()
                        .get("/api/consumer/data")
                        .then()
                        .statusCode(anyOf(is(200), is(204)))
                        .body(is(notNullValue()));
            });
        }

        private JsonObject httpDataAddress(String baseUrl) {
            return createObjectBuilder()
                    .add(TYPE, EDC_NAMESPACE + "DataAddress")
                    .add(EDC_NAMESPACE + "type", "HttpData")
                    .add(EDC_NAMESPACE + "baseUrl", baseUrl)
                    .build();
        }

        private JsonObject noPrivateProperty() {
            return Json.createObjectBuilder().build();
        }

    }
}
