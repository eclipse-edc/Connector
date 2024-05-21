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
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.jetbrains.annotations.NotNull;
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
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.createDatabase;
import static org.eclipse.edc.test.e2e.Runtimes.InMemory.controlPlane;
import static org.eclipse.edc.test.e2e.Runtimes.InMemory.controlPlaneEmbeddedDataPlane;
import static org.eclipse.edc.test.e2e.Runtimes.InMemory.dataPlane;
import static org.eclipse.edc.test.e2e.Runtimes.backendService;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


class TransferPushEndToEndTest {

    abstract static class Tests extends TransferEndToEndTestBase {

        @Test
        void httpPushDataTransfer() {
            seedVaults();
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, noConstraintPolicy(), httpDataAddressProperties());
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
        void httpToHttp_oauth2Provisioning() {
            seedVaults();
            var assetId = UUID.randomUUID().toString();
            var sourceDataAddressProperties = Map.<String, Object>of(
                    "type", "HttpData",
                    "baseUrl", PROVIDER.backendService() + "/api/provider/oauth2data",
                    "oauth2:clientId", "clientId",
                    "oauth2:clientSecretKey", "provision-oauth-secret",
                    "oauth2:tokenUrl", PROVIDER.backendService() + "/api/oauth2/token"
            );

            createResourcesOnProvider(assetId, noConstraintPolicy(), sourceDataAddressProperties);
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

        protected abstract void seedVaults();

        private JsonObject httpDataAddress(String baseUrl) {
            return createObjectBuilder()
                    .add(TYPE, EDC_NAMESPACE + "DataAddress")
                    .add(EDC_NAMESPACE + "type", "HttpData")
                    .add(EDC_NAMESPACE + "baseUrl", baseUrl)
                    .build();
        }

        @NotNull
        private Map<String, Object> httpDataAddressProperties() {
            return Map.of(
                    "name", "transfer-test",
                    "baseUrl", PROVIDER.backendService() + "/api/provider/data",
                    "type", "HttpData",
                    "proxyQueryParams", "true"
            );
        }

        private JsonObject noPrivateProperty() {
            return Json.createObjectBuilder().build();
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        private static final EdcRuntimeExtension CONSUMER_RUNTIME = controlPlane("consumer-control-plane", CONSUMER.controlPlaneConfiguration());
        private static final EdcRuntimeExtension PROVIDER_RUNTIME = controlPlane("provider-control-plane", PROVIDER.controlPlaneConfiguration());
        private static final EdcRuntimeExtension PROVIDER_DATAPLANE = dataPlane("provider-data-plane", PROVIDER.dataPlaneConfiguration());
        @RegisterExtension
        static final EdcClassRuntimesExtension RUNTIMES = new EdcClassRuntimesExtension(
                CONSUMER_RUNTIME,
                backendService("consumer-backend-service", CONSUMER.backendServiceConfiguration()),
                PROVIDER_RUNTIME,
                PROVIDER_DATAPLANE,
                backendService("provider-backend-service", PROVIDER.backendServiceConfiguration())
        );

        @Override
        protected void seedVaults() {
            seedVault(CONSUMER_RUNTIME);
            seedVault(PROVIDER_RUNTIME);
            seedVault(PROVIDER_DATAPLANE);
        }
    }

    @Nested
    @EndToEndTest
    class EmbeddedDataPlane extends Tests {

        private static final EdcRuntimeExtension CONSUMER_RUNTIME = controlPlane("consumer-control-plane", CONSUMER.controlPlaneConfiguration());
        private static final EdcRuntimeExtension PROVIDER_RUNTIME = controlPlaneEmbeddedDataPlane("provider-control-plane", PROVIDER.controlPlaneEmbeddedDataPlaneConfiguration());
        @RegisterExtension
        static final EdcClassRuntimesExtension RUNTIMES = new EdcClassRuntimesExtension(
                CONSUMER_RUNTIME,
                backendService("consumer-backend-service", CONSUMER.backendServiceConfiguration()),
                PROVIDER_RUNTIME,
                backendService("provider-backend-service", PROVIDER.backendServiceConfiguration())
        );


        @Override
        protected void seedVaults() {
            seedVault(CONSUMER_RUNTIME);
            seedVault(PROVIDER_RUNTIME);
        }
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        static final BeforeAllCallback CREATE_DATABASES = context -> {
            createDatabase(CONSUMER.getName());
            createDatabase(PROVIDER.getName());
        };

        private static final EdcRuntimeExtension CONSUMER_RUNTIME = Runtimes.Postgres.controlPlane("consumer-control-plane", CONSUMER.controlPlanePostgresConfiguration());
        private static final EdcRuntimeExtension PROVIDER_RUNTIME = Runtimes.Postgres.controlPlane("provider-control-plane", PROVIDER.controlPlanePostgresConfiguration());
        private static final EdcRuntimeExtension PROVIDER_DATAPLANE = Runtimes.Postgres.dataPlane("provider-data-plane", PROVIDER.dataPlanePostgresConfiguration());
        @RegisterExtension
        static final EdcClassRuntimesExtension RUNTIMES = new EdcClassRuntimesExtension(
                CONSUMER_RUNTIME,
                backendService("consumer-backend-service", CONSUMER.backendServiceConfiguration()),
                PROVIDER_RUNTIME,
                PROVIDER_DATAPLANE,
                backendService("provider-backend-service", PROVIDER.backendServiceConfiguration())
        );

        @Override
        protected void seedVaults() {
            seedVault(CONSUMER_RUNTIME);
            seedVault(PROVIDER_RUNTIME);
            seedVault(PROVIDER_DATAPLANE);
        }
    }

}
