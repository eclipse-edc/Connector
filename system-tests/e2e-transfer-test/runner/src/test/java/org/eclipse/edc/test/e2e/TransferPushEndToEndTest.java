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
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.BinaryBody;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.JsonBody.json;
import static org.mockserver.stop.Stop.stopQuietly;


class TransferPushEndToEndTest {

    abstract static class Tests extends TransferEndToEndTestBase {

        private static ClientAndServer providerDataSource;
        private static ClientAndServer consumerDataDestination;

        @BeforeAll
        static void setUp() {
            providerDataSource = startClientAndServer(getFreePort());
            consumerDataDestination = startClientAndServer(getFreePort());
            consumerDataDestination.when(HttpRequest.request()).respond(HttpResponse.response());
        }

        @AfterAll
        static void afterAll() {
            stopQuietly(providerDataSource);
            stopQuietly(consumerDataDestination);
        }

        @Test
        void httpPushDataTransfer() {
            providerDataSource.when(HttpRequest.request()).respond(HttpResponse.response().withBody("data"));
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, httpSourceDataAddress());

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withDestination(httpDataAddress("http://localhost:" + consumerDataDestination.getPort() + "/destination"))
                    .withTransferType("HttpData-PUSH").execute();

            CONSUMER.awaitTransferToBeInState(transferProcessId, COMPLETED);

            providerDataSource.verify(HttpRequest.request("/source").withMethod("GET"));
            consumerDataDestination.verify(HttpRequest.request("/destination").withBody(BinaryBody.binary("data".getBytes())));
        }

        @Test
        void responseChannelOnPullTransfer() {
            providerDataSource.when(HttpRequest.request()).respond(HttpResponse.response().withBody("data"));
            var assetId = UUID.randomUUID().toString();
            var dataAddressProperties = httpSourceDataAddress();
            var responseChannel = Map.of(
                    EDC_NAMESPACE + "name", "transfer-test",
                    EDC_NAMESPACE + "baseUrl", "http://any/response/channel",
                    EDC_NAMESPACE + "type", "HttpData"
            );
            dataAddressProperties.put(EDC_NAMESPACE + "responseChannel", responseChannel);
            createResourcesOnProvider(assetId, dataAddressProperties);

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withDestination(httpDataAddress("http://localhost:" + consumerDataDestination.getPort() + "/destination"))
                    .withTransferType("HttpData-PUSH-HttpData").execute();

            CONSUMER.awaitTransferToBeInState(transferProcessId, COMPLETED);

            providerDataSource.verify(HttpRequest.request("/source").withMethod("GET"));
            consumerDataDestination.verify(HttpRequest.request("/destination").withBody(BinaryBody.binary("data".getBytes())));

            var edr = await().atMost(timeout).until(() -> CONSUMER.getEdr(transferProcessId), Objects::nonNull);
            await().atMost(timeout).untilAsserted(() -> CONSUMER.postResponse(edr, body -> assertThat(body).isEqualTo("response received")));
        }

        @Test
        void httpToHttp_oauth2Provisioning() {
            var oauth2server = startClientAndServer(getFreePort());
            oauth2server.when(HttpRequest.request()).respond(HttpResponse.response().withBody(json(Map.of("access_token", "token"))));
            providerDataSource.when(HttpRequest.request()).respond(HttpResponse.response().withBody("data"));
            getDataplaneVault().storeSecret("provision-oauth-secret", "supersecret");
            var assetId = UUID.randomUUID().toString();
            var sourceDataAddressProperties = Map.<String, Object>of(
                    "type", "HttpData",
                    "baseUrl", "http://localhost:" + providerDataSource.getPort() + "/source",
                    "oauth2:clientId", "clientId",
                    "oauth2:clientSecretKey", "provision-oauth-secret",
                    "oauth2:tokenUrl", "http://localhost:" + oauth2server.getPort() + "/token"
            );

            createResourcesOnProvider(assetId, sourceDataAddressProperties);

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withDestination(httpDataAddress("http://localhost:" + consumerDataDestination.getPort() + "/destination"))
                    .withTransferType("HttpData-PUSH").execute();

            CONSUMER.awaitTransferToBeInState(transferProcessId, COMPLETED);

            oauth2server.verify(HttpRequest.request("/token").withBody("grant_type=client_credentials&client_secret=supersecret&client_id=clientId"));
            providerDataSource.verify(HttpRequest.request("/source").withMethod("GET").withHeader("Authorization", "Bearer token"));
            consumerDataDestination.verify(HttpRequest.request("/destination").withBody(BinaryBody.binary("data".getBytes())));
            stopQuietly(oauth2server);
        }

        private Map<String, Object> httpSourceDataAddress() {
            return new HashMap<>(Map.of(
                    TYPE, EDC_NAMESPACE + "DataAddress",
                    EDC_NAMESPACE + "type", "HttpData",
                    EDC_NAMESPACE + "baseUrl", "http://localhost:" + providerDataSource.getPort() + "/source",
                    EDC_NAMESPACE + "proxyQueryParams", "true"
            ));
        }

        private JsonObject httpDataAddress(String baseUrl) {
            return createObjectBuilder()
                    .add(TYPE, EDC_NAMESPACE + "DataAddress")
                    .add(EDC_NAMESPACE + "type", "HttpData")
                    .add(EDC_NAMESPACE + "baseUrl", baseUrl)
                    .build();
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = new RuntimePerClassExtension(
                Runtimes.IN_MEMORY_CONTROL_PLANE.create("consumer-control-plane")
                        .configurationProvider(CONSUMER::controlPlaneConfig)
        );

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = new RuntimePerClassExtension(
                Runtimes.IN_MEMORY_CONTROL_PLANE.create("provider-control-plane")
                        .configurationProvider(PROVIDER::controlPlaneConfig)
        );

        @RegisterExtension
        static final RuntimeExtension PROVIDER_DATA_PLANE = new RuntimePerClassExtension(
                Runtimes.IN_MEMORY_DATA_PLANE.create("provider-data-plane")
                        .configurationProvider(PROVIDER::dataPlaneConfig)
        );

        @Override
        protected Vault getDataplaneVault() {
            return PROVIDER_DATA_PLANE.getService(Vault.class);
        }
    }

    @Nested
    @EndToEndTest
    class EmbeddedDataPlane extends Tests {

        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = new RuntimePerClassExtension(
                Runtimes.IN_MEMORY_CONTROL_PLANE.create("consumer-control-plane")
                        .configurationProvider(CONSUMER::controlPlaneConfig)
        );

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = new RuntimePerClassExtension(
                Runtimes.IN_MEMORY_CONTROL_PLANE_EMBEDDED_DATA_PLANE.create("provider-control-plane")
                        .configurationProvider(PROVIDER::controlPlaneEmbeddedDataPlaneConfig)
        );

        @Override
        protected Vault getDataplaneVault() {
            return PROVIDER_CONTROL_PLANE.getService(Vault.class);
        }
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback CREATE_DATABASES = context -> {
            POSTGRESQL_EXTENSION.createDatabase(CONSUMER.getName());
            POSTGRESQL_EXTENSION.createDatabase(PROVIDER.getName());
        };

        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = new RuntimePerClassExtension(
                Runtimes.POSTGRES_CONTROL_PLANE.create("consumer-control-plane")
                        .configurationProvider(CONSUMER::controlPlaneConfig)
                        .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(CONSUMER.getName()))
        );

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = new RuntimePerClassExtension(
                Runtimes.POSTGRES_CONTROL_PLANE.create("provider-control-plane")
                        .configurationProvider(PROVIDER::controlPlaneConfig)
                        .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(PROVIDER.getName()))
        );

        @RegisterExtension
        static final RuntimeExtension PROVIDER_DATA_PLANE = new RuntimePerClassExtension(
                Runtimes.POSTGRES_DATA_PLANE.create("provider-data-plane")
                        .configurationProvider(PROVIDER::dataPlaneConfig)
                        .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(PROVIDER.getName()))
                        .registerSystemExtension(ServiceExtension.class, new HttpProxyDataPlaneExtension())

        );

        @Override
        protected Vault getDataplaneVault() {
            return PROVIDER_DATA_PLANE.getService(Vault.class);
        }
    }

}
