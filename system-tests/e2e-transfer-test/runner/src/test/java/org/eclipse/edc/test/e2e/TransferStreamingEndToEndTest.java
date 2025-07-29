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
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
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
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static java.time.Duration.ZERO;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.contractExpiresIn;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class TransferStreamingEndToEndTest {

    @Testcontainers
    abstract static class Tests extends TransferEndToEndTestBase {

        @RegisterExtension
        static WireMockExtension destinationServer = WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .build();
        
        protected final String sinkTopic = "sink_topic_" + UUID.randomUUID();
        private final String sourceTopic = "source_topic_" + UUID.randomUUID();

        protected abstract KafkaExtension getKafkaExtension();

        @BeforeEach
        void setUp() {
            var producer = getKafkaExtension().createKafkaProducer();

            newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    () -> producer.send(new ProducerRecord<>(sourceTopic, sampleMessage())),
                    0, 100, MILLISECONDS);
        }

        @Test
        void kafkaToHttpTransfer() {

            destinationServer.stubFor(post("/api/service")
                    .willReturn(okJson("{}")));

            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, contractExpiresIn("10s"), kafkaSourceProperty(getKafkaExtension().getBootstrapServers()));

            var destination = httpSink(destinationServer.getPort(), "/api/service");
            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withDestination(destination).withTransferType("HttpData-PUSH").execute();

            await().atMost(timeout).untilAsserted(() -> {
                destinationServer.verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/api/service")));
            });

            CONSUMER.awaitTransferToBeInState(transferProcessId, TERMINATED);

            destinationServer.resetRequests();

            await().pollDelay(5, SECONDS).atMost(timeout).untilAsserted(() -> {
                try {
                    destinationServer.verify(0, postRequestedFor(urlEqualTo("/api/service")));
                } catch (AssertionError assertionError) {
                    destinationServer.resetRequests();
                    throw assertionError;
                }
            });

        }

        @Test
        void kafkaToKafkaTransfer() {
            try (var consumer = getKafkaExtension().createKafkaConsumer()) {
                consumer.subscribe(List.of(sinkTopic));

                var assetId = UUID.randomUUID().toString();
                createResourcesOnProvider(assetId, contractExpiresIn("10s"), kafkaSourceProperty(getKafkaExtension().getBootstrapServers()));

                var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                        .withDestination(kafkaSink(getKafkaExtension().getBootstrapServers())).withTransferType("Kafka-PUSH").execute();
                assertMessagesAreSentTo(consumer);

                CONSUMER.awaitTransferToBeInState(transferProcessId, TERMINATED);
                assertNoMoreMessagesAreSentTo(consumer);
            }
        }

        @Test
        void shouldSuspendAndResumeTransfer() {
            try (var consumer = getKafkaExtension().createKafkaConsumer()) {
                consumer.subscribe(List.of(sinkTopic));

                var assetId = UUID.randomUUID().toString();
                createResourcesOnProvider(assetId, kafkaSourceProperty(getKafkaExtension().getBootstrapServers()));

                var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                        .withDestination(kafkaSink(getKafkaExtension().getBootstrapServers())).withTransferType("Kafka-PUSH").execute();
                assertMessagesAreSentTo(consumer);

                CONSUMER.suspendTransfer(transferProcessId, "any kind of reason");
                CONSUMER.awaitTransferToBeInState(transferProcessId, SUSPENDED);
                assertNoMoreMessagesAreSentTo(consumer);

                CONSUMER.resumeTransfer(transferProcessId);
                CONSUMER.awaitTransferToBeInState(transferProcessId, STARTED);
                assertMessagesAreSentTo(consumer);
            }
        }

        protected void assertMessagesAreSentTo(Consumer<String, String> consumer) {
            await().atMost(timeout).untilAsserted(() -> {
                var records = consumer.poll(ZERO);
                assertThat(records.isEmpty()).isFalse();
                records.records(sinkTopic).forEach(record -> assertThat(record.value()).isEqualTo(sampleMessage()));
            });
        }

        protected void assertNoMoreMessagesAreSentTo(Consumer<String, String> consumer) {
            consumer.poll(ZERO);
            await().pollDelay(5, SECONDS).atMost(timeout).untilAsserted(() -> {
                var recordsFound = consumer.poll(Duration.ofSeconds(1)).records(sinkTopic);
                assertThat(recordsFound).isEmpty();
            });
        }

        private JsonObject httpSink(Integer port, String path) {
            return Json.createObjectBuilder()
                    .add(TYPE, EDC_NAMESPACE + "DataAddress")
                    .add(EDC_NAMESPACE + "type", "HttpData")
                    .add(EDC_NAMESPACE + "properties", Json.createObjectBuilder()
                            .add(EDC_NAMESPACE + "name", "data")
                            .add(EDC_NAMESPACE + "baseUrl", format("http://localhost:%s", port))
                            .add(EDC_NAMESPACE + "path", path)
                            .build())
                    .build();
        }

        protected JsonObject kafkaSink(String bootstrapServers) {
            return Json.createObjectBuilder()
                    .add(TYPE, EDC_NAMESPACE + "DataAddress")
                    .add(EDC_NAMESPACE + "type", "Kafka")
                    .add(EDC_NAMESPACE + "properties", Json.createObjectBuilder()
                            .add(EDC_NAMESPACE + "topic", sinkTopic)
                            .add(EDC_NAMESPACE + kafkaProperty("bootstrap.servers"), bootstrapServers)
                            .build())
                    .build();
        }

        protected Map<String, Object> kafkaSourceProperty(String bootstrapServers) {
            return Map.of(
                    "name", "data",
                    "type", "Kafka",
                    "topic", sourceTopic,
                    kafkaProperty("bootstrap.servers"), bootstrapServers,
                    kafkaProperty("max.poll.records"), "100"
            );
        }

        private String kafkaProperty(String property) {
            return "kafka." + property;
        }

        private String sampleMessage() {
            return "sampleMessage";
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @Order(0)
        @RegisterExtension
        static final KafkaExtension KAFKA_EXTENSION = new KafkaExtension();

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

        @Override
        protected KafkaExtension getKafkaExtension() {
            return KAFKA_EXTENSION;
        }
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final KafkaExtension KAFKA_EXTENSION = new KafkaExtension();

        @Order(1)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();

        @Order(2)
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

        private static final EmbeddedRuntime PROVIDER_DATA_PLANE_RUNTIME = Runtimes.POSTGRES_DATA_PLANE.create("provider-data-plane")
                .configurationProvider(PROVIDER::dataPlaneConfig)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(PROVIDER.getName()));

        @RegisterExtension
        static final RuntimeExtension PROVIDER_DATA_PLANE = new RuntimePerClassExtension(PROVIDER_DATA_PLANE_RUNTIME);

        @Override
        protected Vault getDataplaneVault() {
            return PROVIDER_DATA_PLANE.getService(Vault.class);
        }

        @Override
        protected KafkaExtension getKafkaExtension() {
            return KAFKA_EXTENSION;
        }

        @Test
        void shouldResumeTransfer_whenDataPlaneRestarts() {
            try (var consumer = getKafkaExtension().createKafkaConsumer()) {
                consumer.subscribe(List.of(sinkTopic));

                var assetId = UUID.randomUUID().toString();
                createResourcesOnProvider(assetId, kafkaSourceProperty(getKafkaExtension().getBootstrapServers()));

                var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                        .withDestination(kafkaSink(getKafkaExtension().getBootstrapServers())).withTransferType("Kafka-PUSH").execute();
                assertMessagesAreSentTo(consumer);

                PROVIDER_DATA_PLANE_RUNTIME.shutdown();

                assertNoMoreMessagesAreSentTo(consumer);

                PROVIDER_DATA_PLANE_RUNTIME.boot(false);

                CONSUMER.awaitTransferToBeInState(transferProcessId, STARTED);
                assertMessagesAreSentTo(consumer);
            }
        }
    }

}
