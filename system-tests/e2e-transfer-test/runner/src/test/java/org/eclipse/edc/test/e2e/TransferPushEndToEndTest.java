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
import org.eclipse.edc.junit.annotations.Runtime;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.BeforeAll;
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
        void httpPushDataTransfer(@Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                  @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider) {
            providerDataSource.stubFor(get(anyUrl()).willReturn(ok("data")));
            var assetId = UUID.randomUUID().toString();
            var dataAddressProperties = Map.<String, Object>of(
                    "name", "transfer-test",
                    "baseUrl", "http://localhost:" + providerDataSource.getPort() + "/source",
                    "type", "HttpData",
                    "proxyQueryParams", "true"
            );
            createResourcesOnProvider(provider, assetId, dataAddressProperties);

            var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withDestination(httpDataAddress("http://localhost:" + consumerDataDestination.getPort() + "/destination"))
                    .withTransferType("HttpData-PUSH").execute();

            consumer.awaitTransferToBeInState(transferProcessId, COMPLETED);

            providerDataSource.verify(getRequestedFor(urlEqualTo("/source")));
            consumerDataDestination.verify(postRequestedFor(urlEqualTo("/destination"))
                    .withRequestBody(binaryEqualTo("data".getBytes())));

        }

        @Test
        void httpToHttp_oauth2Provisioning(
                @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                @Runtime(PROVIDER_DP) Vault vault) {

            oauth2server.stubFor(post(anyUrl()).willReturn(okJson("{\"access_token\": \"token\"}")));
            providerDataSource.stubFor(get(anyUrl()).willReturn(ok("data")));
            vault.storeSecret("provision-oauth-secret", "supersecret");
            var assetId = UUID.randomUUID().toString();
            var sourceDataAddressProperties = Map.<String, Object>of(
                    "type", "HttpData",
                    "baseUrl", "http://localhost:" + providerDataSource.getPort() + "/source",
                    "oauth2:clientId", "clientId",
                    "oauth2:clientSecretKey", "provision-oauth-secret",
                    "oauth2:tokenUrl", "http://localhost:" + oauth2server.getPort() + "/token"
            );

            createResourcesOnProvider(provider, assetId, sourceDataAddressProperties);

            var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withDestination(httpDataAddress("http://localhost:" + consumerDataDestination.getPort() + "/destination"))
                    .withTransferType("HttpData-PUSH").execute();

            consumer.awaitTransferToBeInState(transferProcessId, COMPLETED);

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
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_CP)
                .modules(Runtimes.ControlPlane.MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        static final Endpoints PROVIDER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_CP)
                .modules(Runtimes.ControlPlane.MODULES)
                .endpoints(PROVIDER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        @RegisterExtension
        static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.DataPlane.IN_MEM_MODULES)
                .endpoints(Runtimes.DataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.DataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.dataPlaneSelectorFor(PROVIDER_ENDPOINTS))
                .build();

    }

    @Nested
    @EndToEndTest
    class EmbeddedDataPlane extends Tests {


        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_CP)
                .modules(Runtimes.ControlPlane.MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        static final Endpoints PROVIDER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_CP)
                .modules(Runtimes.ControlPlane.EMBEDDED_DP_MODULES)
                .endpoints(PROVIDER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
                .configurationProvider(Runtimes.DataPlane::config)
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        // This is only for satisfying the @Runtime(PROVIDER_DP) in the Tests class,
        // and it's going away eventually
        @RegisterExtension
        static final RuntimeExtension MOCK_DP = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.DataPlane.IN_MEM_MODULES)
                .endpoints(Runtimes.DataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.DataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.dataPlaneSelectorFor(PROVIDER_ENDPOINTS))
                .build();

        @BeforeAll
        static void setup(@Runtime(PROVIDER_CP) Vault vault) {
            vault.storeSecret("provision-oauth-secret", "supersecret");

        }

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {


        static final String CONSUMER_DB = "consumer";
        static final String PROVIDER_DB = "provider";

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback CREATE_DATABASES = context -> {
            POSTGRESQL_EXTENSION.createDatabase(CONSUMER_DB);
            POSTGRESQL_EXTENSION.createDatabase(PROVIDER_DB);
        };

        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_CP)
                .modules(Runtimes.ControlPlane.MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(CONSUMER_DB))
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        static final Endpoints PROVIDER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_CP)
                .modules(Runtimes.ControlPlane.MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(PROVIDER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(PROVIDER_DB))
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        @RegisterExtension
        static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.DataPlane.SQL_MODULES)
                .endpoints(Runtimes.DataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.DataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.dataPlaneSelectorFor(PROVIDER_ENDPOINTS))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(PROVIDER_DB))
                .build();

    }

}
