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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.createDatabase;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;


class TransferPullEndToEndTest {

    abstract static class Tests extends TransferEndToEndTestBase {
        private static final ObjectMapper MAPPER = new ObjectMapper();
        private static final String CALLBACK_PATH = "hooks";
        private static final int CALLBACK_PORT = getFreePort();
        private static ClientAndServer callbacksEndpoint;

        @BeforeEach
        void beforeEach() {
            callbacksEndpoint = startClientAndServer(CALLBACK_PORT);
        }

        @AfterEach
        void tearDown() {
            stopQuietly(callbacksEndpoint);
        }

        @Test
        void httpPull_dataTransfer_withCallbacks() {
            seedVaults();
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, httpDataAddressProperties());
            var dynamicReceiverProps = CONSUMER.dynamicReceiverPrivateProperties();

            var callbacks = Json.createArrayBuilder()
                    .add(createCallback(callbackUrl(), true, Set.of("transfer.process.started")))
                    .build();

            var request = request().withPath("/" + CALLBACK_PATH)
                    .withMethod(HttpMethod.POST.name());

            var events = new ConcurrentHashMap<String, TransferProcessStarted>();

            callbacksEndpoint.when(request).respond(req -> this.cacheEdr(req, events));

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withPrivateProperties(dynamicReceiverProps)
                    .withTransferType("HttpData-PULL")
                    .withCallbacks(callbacks)
                    .execute();

            awaitTransferToBeInState(transferProcessId, STARTED);

            await().atMost(timeout).untilAsserted(() -> assertThat(events.get(transferProcessId)).isNotNull());

            var event = events.get(transferProcessId);
            var msg = UUID.randomUUID().toString();
            await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(event.getDataAddress(), Map.of("message", msg), equalTo(msg)));
        }

        @Test
        void httpPull_dataTransfer_withEdrCache() {
            seedVaults();
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, PolicyFixtures.contractExpiresIn("10s"), httpDataAddressProperties());
            var dynamicReceiverProps = CONSUMER.dynamicReceiverPrivateProperties();

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withPrivateProperties(dynamicReceiverProps)
                    .withTransferType("HttpData-PULL")
                    .execute();

            await().atMost(timeout).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(STARTED.name());
            });

            var edr = await().atMost(timeout).until(() -> CONSUMER.getEdr(transferProcessId), Objects::nonNull);

            // Do the transfer
            var msg = UUID.randomUUID().toString();
            await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of("message", msg), equalTo(msg)));

            // checks that the EDR is gone once the contract expires
            await().atMost(timeout).untilAsserted(() -> assertThatThrownBy(() -> CONSUMER.getEdr(transferProcessId)));

            // checks that transfer fails
            await().atMost(timeout).untilAsserted(() -> assertThatThrownBy(() -> CONSUMER.pullData(edr, Map.of("message", msg), equalTo(msg))));
        }

        @Test
        void suspendAndResume_httpPull_dataTransfer_withEdrCache() {
            seedVaults();
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, httpDataAddressProperties());

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withPrivateProperties(CONSUMER.dynamicReceiverPrivateProperties())
                    .withTransferType("HttpData-PULL")
                    .execute();

            awaitTransferToBeInState(transferProcessId, STARTED);

            var edr = await().atMost(timeout).until(() -> CONSUMER.getEdr(transferProcessId), Objects::nonNull);

            var msg = UUID.randomUUID().toString();
            await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of("message", msg), equalTo(msg)));

            CONSUMER.suspendTransfer(transferProcessId, "supension");

            awaitTransferToBeInState(transferProcessId, SUSPENDED);

            // checks that the EDR is gone once the transfer has been suspended
            await().atMost(timeout).untilAsserted(() -> assertThatThrownBy(() -> CONSUMER.getEdr(transferProcessId)));
            // checks that transfer fails
            await().atMost(timeout).untilAsserted(() -> assertThatThrownBy(() -> CONSUMER.pullData(edr, Map.of("message", msg), equalTo(msg))));

            CONSUMER.resumeTransfer(transferProcessId);

            // check that transfer is available again
            awaitTransferToBeInState(transferProcessId, STARTED);
            var secondEdr = await().atMost(timeout).until(() -> CONSUMER.getEdr(transferProcessId), Objects::nonNull);
            var secondMessage = UUID.randomUUID().toString();
            await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(secondEdr, Map.of("message", secondMessage), equalTo(secondMessage)));

        }

        @Test
        void pullFromHttp_httpProvision() {
            seedVaults();
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, Map.of(
                    "name", "transfer-test",
                    "baseUrl", PROVIDER.backendService() + "/api/provider/data",
                    "type", "HttpProvision",
                    "proxyQueryParams", "true"
            ));

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withPrivateProperties(CONSUMER.dynamicReceiverPrivateProperties())
                    .withTransferType("HttpData-PULL")
                    .execute();

            awaitTransferToBeInState(transferProcessId, STARTED);

            await().atMost(timeout).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(STARTED.name());

                var edr = await().atMost(timeout).until(() -> CONSUMER.getEdr(transferProcessId), Objects::nonNull);
                CONSUMER.pullData(edr, Map.of("message", "some information"), equalTo("some information"));
            });
        }

        @Test
        void shouldTerminateTransfer_whenContractExpires_fixedInForcePeriod() {
            seedVaults();
            var assetId = UUID.randomUUID().toString();
            var now = Instant.now();

            // contract was valid from t-10d to t-5d, so "now" it is expired
            var contractPolicy = inForceDatePolicy("gteq", now.minus(ofDays(10)), "lteq", now.minus(ofDays(5)));
            createResourcesOnProvider(assetId, contractPolicy, httpDataAddressProperties());

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .execute();

            await().atMost(timeout).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(TERMINATED.name());
            });
        }

        @Test
        void shouldTerminateTransfer_whenContractExpires_durationInForcePeriod() {
            seedVaults();
            var assetId = UUID.randomUUID().toString();
            var now = Instant.now();
            // contract was valid from t-10d to t-5d, so "now" it is expired
            var contractPolicy = inForceDatePolicy("gteq", now.minus(ofDays(10)), "lteq", "contractAgreement+1s");
            createResourcesOnProvider(assetId, contractPolicy, httpDataAddressProperties());

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .execute();
            await().atMost(timeout).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(TERMINATED.name());
            });
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

        protected abstract void seedVaults();

        @NotNull
        private Map<String, Object> httpDataAddressProperties() {
            return Map.of(
                    "name", "transfer-test",
                    "baseUrl", PROVIDER.backendService() + "/api/provider/data",
                    "type", "HttpData",
                    "proxyQueryParams", "true"
            );
        }

        private HttpResponse cacheEdr(HttpRequest request, Map<String, TransferProcessStarted> events) {

            try {
                var event = MAPPER.readValue(request.getBody().toString(), new TypeReference<EventEnvelope<TransferProcessStarted>>() {
                });
                events.put(event.getPayload().getTransferProcessId(), event.getPayload());
                return response()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.PLAIN_TEXT_UTF_8.toString())
                        .withBody("{}");

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }

        private String callbackUrl() {
            return String.format("http://localhost:%d/%s", callbacksEndpoint.getLocalPort(), CALLBACK_PATH);
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
                Runtimes.IN_MEMORY_CONTROL_PLANE_EMBEDDED_DATA_PLANE.create("consumer-control-plane", CONSUMER.controlPlaneEmbeddedDataPlaneConfiguration()));

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
