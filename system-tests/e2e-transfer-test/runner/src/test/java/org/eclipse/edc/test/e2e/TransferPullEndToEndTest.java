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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.annotations.Runtime;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.time.Duration.ofDays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.inForceDatePolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;


class TransferPullEndToEndTest {

    abstract static class Tests extends TransferEndToEndTestBase {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        @RegisterExtension
        static WireMockExtension callbacksEndpoint = WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .build();


        private static @NotNull Map<String, Object> httpSourceDataAddress() {
            return new HashMap<>(Map.of(
                    EDC_NAMESPACE + "name", "transfer-test",
                    EDC_NAMESPACE + "baseUrl", "http://any/source",
                    EDC_NAMESPACE + "type", "HttpData"
            ));
        }

        @BeforeAll
        static void setup(@Runtime(PROVIDER_DP) Vault vault) {
            vault.storeSecret("private-key", privateKey);
            vault.storeSecret("public-key", publicKey);
        }

        @Test
        void httpPull_dataTransfer_withCallbacks(@Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                 @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider) throws IOException {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(provider, assetId, httpSourceDataAddress());

            var callbackUrl = String.format("http://localhost:%d/hooks", callbacksEndpoint.getPort());
            var callbacks = Json.createArrayBuilder()
                    .add(createCallback(callbackUrl, true, Set.of("transfer.process.started")))
                    .build();


            callbacksEndpoint.stubFor(post("/hooks").willReturn(okJson("{}")));

            var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .withCallbacks(callbacks)
                    .execute();

            consumer.awaitTransferToBeInState(transferProcessId, STARTED);

            await().atMost(timeout).untilAsserted(() -> callbacksEndpoint.verify(postRequestedFor(urlEqualTo("/hooks"))));

            var requests = callbacksEndpoint.getAllServeEvents();
            assertThat(requests).hasSize(1);
            var request = requests.get(0).getRequest();

            var event = MAPPER.readValue(request.getBody(), new TypeReference<EventEnvelope<TransferProcessStarted>>() {
            });

            var msg = UUID.randomUUID().toString();
            await().atMost(timeout).untilAsserted(() -> consumer.pullData(event.getPayload().getDataAddress(), Map.of("message", msg), body -> assertThat(body).isEqualTo("data")));

        }

        @Test
        void httpPull_dataTransfer_withEdrCache(@Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider) {
            var assetId = UUID.randomUUID().toString();
            var sourceDataAddress = httpSourceDataAddress();
            createResourcesOnProvider(provider, assetId, PolicyFixtures.contractExpiresIn("10s"), sourceDataAddress);

            var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();

            consumer.awaitTransferToBeInState(transferProcessId, STARTED);

            var edrEntry = assertConsumerCanAccessData(consumer, transferProcessId);

            assertConsumerCanNotAccessData(consumer, transferProcessId, edrEntry);
        }

        @Test
        void httpPull_dataTransfer_withHttpResponseChannel(@Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                           @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider) {
            var assetId = UUID.randomUUID().toString();
            var responseChannel = Map.of(
                    EDC_NAMESPACE + "name", "transfer-test",
                    EDC_NAMESPACE + "baseUrl", "http://any/response/channel",
                    EDC_NAMESPACE + "type", "HttpData"
            );
            var sourceDataAddress = httpSourceDataAddress();
            sourceDataAddress.put(EDC_NAMESPACE + "responseChannel", responseChannel);
            createResourcesOnProvider(provider, assetId, PolicyFixtures.contractExpiresIn("30s"), sourceDataAddress);

            var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL-HttpData")
                    .execute();

            consumer.awaitTransferToBeInState(transferProcessId, STARTED);
            assertConsumerCanSendResponse(consumer, transferProcessId);

        }

        @Test
        void suspendAndResumeByConsumer_httpPull_dataTransfer_withEdrCache(@Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                                           @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider) {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(provider, assetId, httpSourceDataAddress());

            var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();

            consumer.awaitTransferToBeInState(transferProcessId, STARTED);

            var edrEntry = assertConsumerCanAccessData(consumer, transferProcessId);

            consumer.suspendTransfer(transferProcessId, "suspension");

            consumer.awaitTransferToBeInState(transferProcessId, SUSPENDED);
            assertConsumerCanNotAccessData(consumer, transferProcessId, edrEntry);

            consumer.resumeTransfer(transferProcessId);

            consumer.awaitTransferToBeInState(transferProcessId, STARTED);
            assertConsumerCanAccessData(consumer, transferProcessId);
        }

        @Test
        void suspendAndResumeByProvider_httpPull_dataTransfer_withEdrCache(@Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                                           @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider) {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(provider, assetId, httpSourceDataAddress());

            var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();

            consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

            var edrEntry = assertConsumerCanAccessData(consumer, consumerTransferProcessId);

            var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);

            provider.suspendTransfer(providerTransferProcessId, "suspension");

            provider.awaitTransferToBeInState(providerTransferProcessId, SUSPENDED);
            assertConsumerCanNotAccessData(consumer, consumerTransferProcessId, edrEntry);

            provider.resumeTransfer(providerTransferProcessId);

            // check that transfer is available again
            provider.awaitTransferToBeInState(providerTransferProcessId, STARTED);
            assertConsumerCanAccessData(consumer, consumerTransferProcessId);
        }

        @Test
        void shouldTerminateTransfer_whenContractExpires_fixedInForcePeriod(@Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                                            @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider) {
            var assetId = UUID.randomUUID().toString();
            var now = Instant.now();
            // contract was valid from t-10d to t-5d, so "now" it is expired
            var contractPolicy = inForceDatePolicy("gteq", now.minus(ofDays(10)), "lteq", now.minus(ofDays(5)));
            createResourcesOnProvider(provider, assetId, contractPolicy, httpSourceDataAddress());

            var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();

            consumer.awaitTransferToBeInState(transferProcessId, TERMINATED);
        }

        @Test
        void shouldTerminateTransfer_whenContractExpires_durationInForcePeriod(@Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                                               @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider) {
            var assetId = UUID.randomUUID().toString();
            var now = Instant.now();
            // contract was valid from t-10d to t-5d, so "now" it is expired
            var contractPolicy = inForceDatePolicy("gteq", now.minus(ofDays(10)), "lteq", "contractAgreement+1s");
            createResourcesOnProvider(provider, assetId, contractPolicy, httpSourceDataAddress());

            var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();

            consumer.awaitTransferToBeInState(transferProcessId, TERMINATED);
        }

        @Test
        void shouldTerminateTransfer_whenProviderTerminatesIt(@Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                              @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider) {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(provider, assetId, httpSourceDataAddress());

            var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();

            consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

            var edrEntry = assertConsumerCanAccessData(consumer, consumerTransferProcessId);

            var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);

            provider.terminateTransfer(providerTransferProcessId);

            provider.awaitTransferToBeInState(providerTransferProcessId, TERMINATED);
            consumer.awaitTransferToBeInState(consumerTransferProcessId, TERMINATED);

            assertConsumerCanNotAccessData(consumer, consumerTransferProcessId, edrEntry);
        }

        @Test
        void shouldTerminateTransfer_whenConsumerTerminatesIt(@Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                              @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider) {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(provider, assetId, httpSourceDataAddress());

            var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();

            consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

            var edrEntry = assertConsumerCanAccessData(consumer, consumerTransferProcessId);

            var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);

            consumer.terminateTransfer(consumerTransferProcessId);

            provider.awaitTransferToBeInState(providerTransferProcessId, TERMINATED);
            consumer.awaitTransferToBeInState(consumerTransferProcessId, TERMINATED);

            assertConsumerCanNotAccessData(consumer, consumerTransferProcessId, edrEntry);
        }

        public JsonObject createCallback(String url, boolean transactional, Set<String> events) {
            return Json.createObjectBuilder()
                    .add(TYPE, EDC_NAMESPACE + "CallbackAddress")
                    .add(EDC_NAMESPACE + "transactional", transactional)
                    .add(EDC_NAMESPACE + "uri", url)
                    .add(EDC_NAMESPACE + "events", events
                            .stream()
                            .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                            .build())
                    .build();
        }

        private EdrMessage assertConsumerCanAccessData(TransferEndToEndParticipant consumer, String consumerTransferProcessId) {
            var edr = await().atMost(timeout).until(() -> consumer.getEdr(consumerTransferProcessId), Objects::nonNull);
            var msg = UUID.randomUUID().toString();
            await().atMost(timeout).untilAsserted(() -> consumer.pullData(edr, Map.of("message", msg), body -> assertThat(body).isEqualTo("data")));

            return new EdrMessage(edr, msg);
        }

        private void assertConsumerCanNotAccessData(TransferEndToEndParticipant consumer, String consumerTransferProcessId, EdrMessage edrMessage) {
            await().atMost(timeout).untilAsserted(() -> assertThatThrownBy(() -> consumer.getEdr(consumerTransferProcessId)));
            await().atMost(timeout).untilAsserted(() -> assertThatThrownBy(
                            () -> consumer.pullData(edrMessage.address(), Map.of("message", edrMessage.message()),
                                    body -> assertThat(body).isEqualTo("data"))
                    )
            );
        }

        private void assertConsumerCanSendResponse(TransferEndToEndParticipant consumer, String consumerTransferProcessId) {
            var edr = await().atMost(timeout).until(() -> consumer.getEdr(consumerTransferProcessId), Objects::nonNull);
            await().atMost(timeout).untilAsserted(() -> consumer.postResponse(edr, body -> assertThat(body).isEqualTo("response received")));
        }

        private record EdrMessage(DataAddress address, String message) {
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
                .build()
                .registerSystemExtension(ServiceExtension.class, new HttpProxyDataPlaneExtension());

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
                .build()
                .registerSystemExtension(ServiceExtension.class, new HttpProxyDataPlaneExtension());


        // This is only for satisfying the @Runtime(PROVIDER_DP) in the Tests class,
        // and it's going away eventually
        @RegisterExtension
        static final RuntimeExtension MOCK_DP = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.DataPlane.IN_MEM_MODULES)
                .endpoints(Runtimes.DataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.DataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.dataPlaneSelectorFor(PROVIDER_ENDPOINTS))
                .build()
                .registerSystemExtension(ServiceExtension.class, new HttpProxyDataPlaneExtension());

        @BeforeAll
        static void setup(@Runtime(PROVIDER_CP) Vault vault) {
            vault.storeSecret("private-key", privateKey);
            vault.storeSecret("public-key", publicKey);
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
                .build()
                .registerSystemExtension(ServiceExtension.class, new HttpProxyDataPlaneExtension());

    }

}
