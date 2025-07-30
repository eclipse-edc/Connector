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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import jakarta.json.JsonObject;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;


class TransferPushEndToEndTest {

    abstract static class Tests extends TransferEndToEndTestBase {

        @RegisterExtension
        static WireMockExtension providerDataSource = WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .build();

        @RegisterExtension
        static WireMockExtension consumerDataDestination = WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .build();

        @RegisterExtension
        static WireMockExtension oauth2server = WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .build();


        @BeforeEach
        void beforeEach() {
            consumerDataDestination.stubFor(post(anyUrl()).willReturn(ok()));

        }

        @Test
        void httpPushDataTransfer() {
            providerDataSource.stubFor(get(anyUrl()).willReturn(ok("data")));
            var assetId = UUID.randomUUID().toString();
            var dataAddressProperties = Map.<String, Object>of(
                    "name", "transfer-test",
                    "baseUrl", "http://localhost:" + providerDataSource.getPort() + "/source",
                    "type", "HttpData",
                    "proxyQueryParams", "true"
            );
            createResourcesOnProvider(assetId, dataAddressProperties);

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withDestination(httpDataAddress("http://localhost:" + consumerDataDestination.getPort() + "/destination"))
                    .withTransferType("HttpData-PUSH").execute();

            CONSUMER.awaitTransferToBeInState(transferProcessId, COMPLETED);

            providerDataSource.verify(getRequestedFor(urlEqualTo("/source")));
            consumerDataDestination.verify(postRequestedFor(urlEqualTo("/destination"))
                    .withRequestBody(binaryEqualTo("data".getBytes())));

        }

        @Test
        void httpToHttp_oauth2Provisioning() {

            oauth2server.stubFor(post(anyUrl()).willReturn(okJson("{\"access_token\": \"token\"}")));
            providerDataSource.stubFor(get(anyUrl()).willReturn(ok("data")));
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

            oauth2server.verify(postRequestedFor(urlEqualTo("/token"))
                    .withRequestBody(equalTo("grant_type=client_credentials&client_secret=supersecret&client_id=clientId")));

            providerDataSource.verify(getRequestedFor(urlEqualTo("/source"))
                    .withHeader("Authorization", equalTo("Bearer token")));

            consumerDataDestination.verify(postRequestedFor(urlEqualTo("/destination"))
                    .withRequestBody(equalTo("data")));

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
        );

        @Override
        protected Vault getDataplaneVault() {
            return PROVIDER_DATA_PLANE.getService(Vault.class);
        }
    }

}
