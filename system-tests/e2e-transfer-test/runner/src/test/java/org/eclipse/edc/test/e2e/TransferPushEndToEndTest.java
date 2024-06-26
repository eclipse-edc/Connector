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

import jakarta.json.JsonObject;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.createDatabase;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


class TransferPushEndToEndTest {

    abstract static class Tests extends TransferEndToEndTestBase {

        @Test
        void httpPushDataTransfer() {
            seedVaults();
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, httpDataAddressProperties());
            var destination = httpDataAddress(CONSUMER.backendService() + "/api/consumer/store");

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withDestination(destination).withTransferType("HttpData-PUSH").execute();

            awaitTransferToBeInState(transferProcessId, COMPLETED);

            given()
                    .baseUri(CONSUMER.backendService().toString())
                    .when()
                    .get("/api/consumer/data")
                    .then()
                    .statusCode(anyOf(is(200), is(204)))
                    .body(is(notNullValue()));
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

            createResourcesOnProvider(assetId, sourceDataAddressProperties);
            var destination = httpDataAddress(CONSUMER.backendService() + "/api/consumer/store");

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withDestination(destination).withTransferType("HttpData-PUSH").execute();

            awaitTransferToBeInState(transferProcessId, COMPLETED);

            given()
                    .baseUri(CONSUMER.backendService().toString())
                    .when()
                    .get("/api/consumer/data")
                    .then()
                    .statusCode(anyOf(is(200), is(204)))
                    .body(is(notNullValue()));
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
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = new RuntimePerClassExtension(
                    Runtimes.IN_MEMORY_CONTROL_PLANE.create("consumer-control-plane", CONSUMER.controlPlaneConfiguration()));

        @RegisterExtension
        static final RuntimeExtension CONSUMER_BACKEND_SERVICE = new RuntimePerClassExtension(
                    Runtimes.BACKEND_SERVICE.create("consumer-backend-service", CONSUMER.backendServiceConfiguration()));

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = new RuntimePerClassExtension(
                    Runtimes.IN_MEMORY_CONTROL_PLANE.create("provider-control-plane", PROVIDER.controlPlaneConfiguration()));

        @RegisterExtension
        static final RuntimeExtension PROVIDER_DATA_PLANE = new RuntimePerClassExtension(
                    Runtimes.IN_MEMORY_DATA_PLANE.create("provider-data-plane", PROVIDER.dataPlaneConfiguration()));

        @RegisterExtension
        static final RuntimeExtension PROVIDER_BACKEND_SERVICE = new RuntimePerClassExtension(
                    Runtimes.BACKEND_SERVICE.create("provider-backend-service", PROVIDER.backendServiceConfiguration()));

        @Override
        protected void seedVaults() {
            seedVault(CONSUMER_CONTROL_PLANE);
            seedVault(PROVIDER_CONTROL_PLANE);
            seedVault(PROVIDER_DATA_PLANE);
        }
    }

    @Nested
    @EndToEndTest
    class EmbeddedDataPlane extends Tests {

        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = new RuntimePerClassExtension(
                    Runtimes.IN_MEMORY_CONTROL_PLANE_EMBEDDED_DATA_PLANE.create("consumer-control-plane", CONSUMER.controlPlaneConfiguration()));

        @RegisterExtension
        static final RuntimeExtension CONSUMER_BACKEND_SERVICE = new RuntimePerClassExtension(
                    Runtimes.BACKEND_SERVICE.create("consumer-backend-service", CONSUMER.backendServiceConfiguration()));

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = new RuntimePerClassExtension(
                    Runtimes.IN_MEMORY_CONTROL_PLANE_EMBEDDED_DATA_PLANE.create("provider-control-plane", PROVIDER.controlPlaneEmbeddedDataPlaneConfiguration()));

        @RegisterExtension
        static final RuntimeExtension PROVIDER_BACKEND_SERVICE = new RuntimePerClassExtension(
                    Runtimes.BACKEND_SERVICE.create("provider-backend-service", PROVIDER.backendServiceConfiguration()));

        @Override
        protected void seedVaults() {
            seedVault(CONSUMER_CONTROL_PLANE);
            seedVault(PROVIDER_CONTROL_PLANE);
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

        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = new RuntimePerClassExtension(
                    Runtimes.POSTGRES_CONTROL_PLANE.create("consumer-control-plane", CONSUMER.controlPlanePostgresConfiguration()));

        @RegisterExtension
        static final RuntimeExtension CONSUMER_BACKEND_SERVICE = new RuntimePerClassExtension(
                    Runtimes.BACKEND_SERVICE.create("consumer-backend-service", CONSUMER.backendServiceConfiguration()));

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = new RuntimePerClassExtension(
                    Runtimes.POSTGRES_CONTROL_PLANE.create("provider-control-plane", PROVIDER.controlPlanePostgresConfiguration()));

        @RegisterExtension
        static final RuntimeExtension PROVIDER_DATA_PLANE = new RuntimePerClassExtension(
                    Runtimes.POSTGRES_DATA_PLANE.create("provider-data-plane", PROVIDER.dataPlanePostgresConfiguration()));

        @RegisterExtension
        static final RuntimeExtension PROVIDER_BACKEND_SERVICE = new RuntimePerClassExtension(
                    Runtimes.BACKEND_SERVICE.create("provider-backend-service", PROVIDER.backendServiceConfiguration()));

        @Override
        protected void seedVaults() {
            seedVault(CONSUMER_CONTROL_PLANE);
            seedVault(PROVIDER_CONTROL_PLANE);
            seedVault(PROVIDER_DATA_PLANE);
        }
    }

}
