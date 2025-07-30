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
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.time.Duration.ofDays;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.inForceDatePolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
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
            return Map.of(
                    EDC_NAMESPACE + "name", "transfer-test",
                    EDC_NAMESPACE + "baseUrl", "http://any/source",
                    EDC_NAMESPACE + "type", "HttpData"
            );
        }

        @Test
        void httpPull_dataTransfer_withCallbacks() throws IOException {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, httpSourceDataAddress());

            var callbackUrl = String.format("http://localhost:%d/hooks", callbacksEndpoint.getPort());
            var callbacks = Json.createArrayBuilder()
                    .add(createCallback(callbackUrl, true, Set.of("transfer.process.started")))
                    .build();


            callbacksEndpoint.stubFor(post("/hooks").willReturn(okJson("{}")));

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .withCallbacks(callbacks)
                    .execute();

            CONSUMER.awaitTransferToBeInState(transferProcessId, STARTED);

            await().atMost(timeout).untilAsserted(() -> callbacksEndpoint.verify(postRequestedFor(urlEqualTo("/hooks"))));

            var requests = callbacksEndpoint.getAllServeEvents();
            assertThat(requests).hasSize(1);
            var request = requests.get(0).getRequest();

            var event = MAPPER.readValue(request.getBody(), new TypeReference<EventEnvelope<TransferProcessStarted>>() {
            });

            var msg = UUID.randomUUID().toString();
            await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(event.getPayload().getDataAddress(), Map.of("message", msg), body -> assertThat(body).isEqualTo("data")));

        }

        @Test
        void httpPull_dataTransfer_withEdrCache() {
            var assetId = UUID.randomUUID().toString();
            var sourceDataAddress = httpSourceDataAddress();
            createResourcesOnProvider(assetId, PolicyFixtures.contractExpiresIn("10s"), sourceDataAddress);

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .execute();

            CONSUMER.awaitTransferToBeInState(transferProcessId, STARTED);

            var edrEntry = assertConsumerCanAccessData(transferProcessId);

            assertConsumerCanNotAccessData(transferProcessId, edrEntry);
        }

        @Test
        void suspendAndResumeByConsumer_httpPull_dataTransfer_withEdrCache() {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, httpSourceDataAddress());

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .execute();

            CONSUMER.awaitTransferToBeInState(transferProcessId, STARTED);

            var edrEntry = assertConsumerCanAccessData(transferProcessId);

            CONSUMER.suspendTransfer(transferProcessId, "supension");

            CONSUMER.awaitTransferToBeInState(transferProcessId, SUSPENDED);
            assertConsumerCanNotAccessData(transferProcessId, edrEntry);

            CONSUMER.resumeTransfer(transferProcessId);

            CONSUMER.awaitTransferToBeInState(transferProcessId, STARTED);
            assertConsumerCanAccessData(transferProcessId);
        }

        @Test
        void suspendAndResumeByProvider_httpPull_dataTransfer_withEdrCache() {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, httpSourceDataAddress());

            var consumerTransferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .execute();

            CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

            var edrEntry = assertConsumerCanAccessData(consumerTransferProcessId);

            var providerTransferProcessId = PROVIDER.getTransferProcesses().stream()
                    .filter(filter -> filter.asJsonObject().getString("correlationId").equals(consumerTransferProcessId))
                    .map(id -> id.asJsonObject().getString("@id")).findFirst().orElseThrow();

            PROVIDER.suspendTransfer(providerTransferProcessId, "supension");

            PROVIDER.awaitTransferToBeInState(providerTransferProcessId, SUSPENDED);
            assertConsumerCanNotAccessData(consumerTransferProcessId, edrEntry);

            PROVIDER.resumeTransfer(providerTransferProcessId);

            // check that transfer is available again
            PROVIDER.awaitTransferToBeInState(providerTransferProcessId, STARTED);
            assertConsumerCanAccessData(consumerTransferProcessId);
        }

        @Test
        void pullFromHttp_httpProvision() {

            var provisionServer = new WireMockServer(options().port(PROVIDER.getHttpProvisionerPort()));

            provisionServer.stubFor(post(urlEqualTo("/provision")).willReturn(ok()));
            provisionServer.start();

            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, Map.of(
                    "name", "transfer-test",
                    "baseUrl", "http://localhost:" + provisionServer.port() + "/provision",
                    "type", "HttpProvision",
                    "proxyQueryParams", "true"
            ));

            var consumerTransferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .execute();

            CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, REQUESTED);

            var providerTransferProcessId = new AtomicReference<String>();

            await().untilAsserted(() -> {
                var ptId = PROVIDER.getTransferProcesses().stream()
                        .filter(filter -> filter.asJsonObject().getString("correlationId").equals(consumerTransferProcessId))
                        .map(id -> id.asJsonObject().getString("@id")).findFirst().orElseThrow();

                assertThat(ptId).isNotNull();
                providerTransferProcessId.set(ptId);
            });

            provision(provisionServer);

            provisionServer.verify(postRequestedFor(urlEqualTo("/provision")));
            provisionServer.resetRequests();

            CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

            assertConsumerCanAccessData(consumerTransferProcessId);

            PROVIDER.terminateTransfer(providerTransferProcessId.get());
            PROVIDER.awaitTransferToBeInState(providerTransferProcessId.get(), DEPROVISIONED);

            provisionServer.verify(postRequestedFor(urlEqualTo("/provision")));

            provisionServer.stop();
        }

        @Test
        void shouldTerminateTransfer_whenContractExpires_fixedInForcePeriod() {
            var assetId = UUID.randomUUID().toString();
            var now = Instant.now();
            // contract was valid from t-10d to t-5d, so "now" it is expired
            var contractPolicy = inForceDatePolicy("gteq", now.minus(ofDays(10)), "lteq", now.minus(ofDays(5)));
            createResourcesOnProvider(assetId, contractPolicy, httpSourceDataAddress());

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .execute();

            CONSUMER.awaitTransferToBeInState(transferProcessId, TERMINATED);
        }

        @Test
        void shouldTerminateTransfer_whenContractExpires_durationInForcePeriod() {
            var assetId = UUID.randomUUID().toString();
            var now = Instant.now();
            // contract was valid from t-10d to t-5d, so "now" it is expired
            var contractPolicy = inForceDatePolicy("gteq", now.minus(ofDays(10)), "lteq", "contractAgreement+1s");
            createResourcesOnProvider(assetId, contractPolicy, httpSourceDataAddress());

            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .execute();

            CONSUMER.awaitTransferToBeInState(transferProcessId, TERMINATED);
        }

        @Test
        void shouldTerminateTransfer_whenProviderTerminatesIt() {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, httpSourceDataAddress());

            var consumerTransferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .execute();

            CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

            var edrEntry = assertConsumerCanAccessData(consumerTransferProcessId);

            var providerTransferProcessId = PROVIDER.getTransferProcesses().stream()
                    .filter(filter -> filter.asJsonObject().getString("correlationId").equals(consumerTransferProcessId))
                    .map(id -> id.asJsonObject().getString("@id")).findFirst().orElseThrow();

            PROVIDER.terminateTransfer(providerTransferProcessId);

            PROVIDER.awaitTransferToBeInState(providerTransferProcessId, DEPROVISIONED);
            CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, TERMINATED);

            assertConsumerCanNotAccessData(consumerTransferProcessId, edrEntry);
        }

        @Test
        void shouldTerminateTransfer_whenConsumerTerminatesIt() {
            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, httpSourceDataAddress());

            var consumerTransferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .execute();

            CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

            var edrEntry = assertConsumerCanAccessData(consumerTransferProcessId);

            var providerTransferProcessId = PROVIDER.getTransferProcesses().stream()
                    .filter(filter -> filter.asJsonObject().getString("correlationId").equals(consumerTransferProcessId))
                    .map(id -> id.asJsonObject().getString("@id")).findFirst().orElseThrow();

            CONSUMER.terminateTransfer(consumerTransferProcessId);

            PROVIDER.awaitTransferToBeInState(providerTransferProcessId, DEPROVISIONED);
            CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, TERMINATED);

            assertConsumerCanNotAccessData(consumerTransferProcessId, edrEntry);
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

        private EdrMessage assertConsumerCanAccessData(String consumerTransferProcessId) {
            var edr = await().atMost(timeout).until(() -> CONSUMER.getEdr(consumerTransferProcessId), Objects::nonNull);
            var msg = UUID.randomUUID().toString();
            await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of("message", msg), body -> assertThat(body).isEqualTo("data")));

            return new EdrMessage(edr, msg);
        }

        private void assertConsumerCanNotAccessData(String consumerTransferProcessId, EdrMessage edrMessage) {
            await().atMost(timeout).untilAsserted(() -> assertThatThrownBy(() -> CONSUMER.getEdr(consumerTransferProcessId)));
            await().atMost(timeout).untilAsserted(() -> assertThatThrownBy(
                            () -> CONSUMER.pullData(edrMessage.address(), Map.of("message", edrMessage.message()),
                                    body -> assertThat(body).isEqualTo("data"))
                    )
            );
        }

        private void provision(WireMockServer provisionServer) {
            var events = provisionServer.getAllServeEvents();
            assertThat(events).hasSize(1);
            var request = events.get(0).getRequest();
            try {
                var requestBody = MAPPER.readValue(request.getBody(), Map.class);

                if ("provision".equals(requestBody.get("type"))) {
                    var callbackRequestBody = Map.of(
                            "edctype", "dataspaceconnector:provisioner-callback-request",
                            "resourceDefinitionId", requestBody.get("resourceDefinitionId"),
                            "assetId", requestBody.get("assetId"),
                            "resourceName", "aName",
                            "contentDataAddress", Map.of("properties", httpSourceDataAddress()),
                            "apiKeyJwt", "unused",
                            "hasToken", false
                    );

                    Executors.newScheduledThreadPool(1).schedule(() -> {
                        try {
                            var provisionRequest = new Request.Builder()
                                    .url("%s/%s/provision".formatted(requestBody.get("callbackAddress"), requestBody.get("transferProcessId")))
                                    .post(RequestBody.create(MAPPER.writeValueAsString(callbackRequestBody), MediaType.get("application/json")))
                                    .build();

                            testHttpClient().execute(provisionRequest).close();
                        } catch (Exception e) {
                            throw new EdcException(e);
                        }
                    }, 1, SECONDS);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        private record EdrMessage(DataAddress address, String message) {
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
                        .registerSystemExtension(ServiceExtension.class, new HttpProxyDataPlaneExtension())
        );

        @Override
        protected Vault getDataplaneVault() {
            return PROVIDER_DATA_PLANE.getService(Vault.class);
        }

    }

    @Nested
    @EndToEndTest
    class InMemoryV2024Rev1 extends Tests {

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
                        .registerSystemExtension(ServiceExtension.class, new HttpProxyDataPlaneExtension())
        );

        // TODO: replace with something better. Temporary hack
        @BeforeAll
        static void beforeAll() {
            CONSUMER.setProtocol("dataspace-protocol-http:2024/1", "/2024/1");
            PROVIDER.setProtocol("dataspace-protocol-http:2024/1", "/2024/1");
        }

        @AfterAll
        static void afterAll() {
            CONSUMER.setProtocol("dataspace-protocol-http");
            PROVIDER.setProtocol("dataspace-protocol-http");
        }

        @Override
        protected Vault getDataplaneVault() {
            return PROVIDER_DATA_PLANE.getService(Vault.class);
        }
    }

    @Nested
    @EndToEndTest
    class InMemoryV2024Rev1WellKnownPath extends Tests {


        @RegisterExtension
        static final RuntimeExtension PROVIDER_DATA_PLANE = new RuntimePerClassExtension(
                Runtimes.IN_MEMORY_DATA_PLANE.create("provider-data-plane")
                        .configurationProvider(PROVIDER::dataPlaneConfig)
                        .registerSystemExtension(ServiceExtension.class, new HttpProxyDataPlaneExtension())
        );

        private static final Function<Supplier<Config>, Supplier<Config>> CONFIG_SUPPLIER = supplier -> () -> {
            var settings = Map.of("edc.dsp.well-known-path.enabled", "true");
            return ConfigFactory.fromMap(settings).merge(supplier.get());
        };

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = new RuntimePerClassExtension(
                Runtimes.IN_MEMORY_CONTROL_PLANE.create("provider-control-plane")
                        .configurationProvider(CONFIG_SUPPLIER.apply(PROVIDER::controlPlaneConfig))
        );
        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = new RuntimePerClassExtension(
                Runtimes.IN_MEMORY_CONTROL_PLANE.create("consumer-control-plane")
                        .configurationProvider(CONFIG_SUPPLIER.apply(CONSUMER::controlPlaneConfig))
        );

        // TODO: replace with something better. Temporary hack
        @BeforeAll
        static void beforeAll() {
            CONSUMER.setProtocol("dataspace-protocol-http:2024/1");
            PROVIDER.setProtocol("dataspace-protocol-http:2024/1");
        }

        @AfterAll
        static void afterAll() {
            CONSUMER.setProtocol("dataspace-protocol-http");
            PROVIDER.setProtocol("dataspace-protocol-http");
        }

        @Override
        protected Vault getDataplaneVault() {
            return PROVIDER_DATA_PLANE.getService(Vault.class);
        }
    }

    @Nested
    @EndToEndTest
    class InMemoryV2025Rev1 extends Tests {

        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = new RuntimePerClassExtension(
                Runtimes.IN_MEMORY_CONTROL_PLANE.create("consumer-control-plane")
                        .configurationProvider(CONSUMER::controlPlaneConfig));

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = new RuntimePerClassExtension(
                Runtimes.IN_MEMORY_CONTROL_PLANE.create("provider-control-plane")
                        .configurationProvider(PROVIDER::controlPlaneConfig));
        @RegisterExtension
        static final RuntimeExtension PROVIDER_DATA_PLANE = new RuntimePerClassExtension(
                Runtimes.IN_MEMORY_DATA_PLANE.create("provider-data-plane")
                        .configurationProvider(PROVIDER::dataPlaneConfig)
                        .registerSystemExtension(ServiceExtension.class, new HttpProxyDataPlaneExtension()));

        private static JsonLd jsonLd;

        // TODO: replace with something better. Temporary hack
        @BeforeAll
        static void beforeAll() {
            jsonLd = CONSUMER.getJsonLd();
            CONSUMER.setJsonLd(CONSUMER_CONTROL_PLANE.getService(JsonLd.class));
            CONSUMER.setProtocol("dataspace-protocol-http:2025-1", "/2025-1");
            PROVIDER.setProtocol("dataspace-protocol-http:2025-1", "/2025-1");
        }

        @AfterAll
        static void afterAll() {
            CONSUMER.setJsonLd(jsonLd);
            CONSUMER.setProtocol("dataspace-protocol-http");
            PROVIDER.setProtocol("dataspace-protocol-http");
        }

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
                Runtimes.IN_MEMORY_CONTROL_PLANE_EMBEDDED_DATA_PLANE.create("consumer-control-plane")
                        .configurationProvider(CONSUMER::controlPlaneEmbeddedDataPlaneConfig)
        );

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = new RuntimePerClassExtension(
                Runtimes.IN_MEMORY_CONTROL_PLANE_EMBEDDED_DATA_PLANE.create("provider-control-plane")
                        .configurationProvider(PROVIDER::controlPlaneEmbeddedDataPlaneConfig)
                        .registerSystemExtension(ServiceExtension.class, new HttpProxyDataPlaneExtension())
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
