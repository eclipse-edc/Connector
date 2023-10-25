/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpMethod;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.test.e2e.annotations.KafkaIntegrationTest;
import org.eclipse.edc.test.e2e.participant.EndToEndTransferParticipant;
import org.eclipse.edc.test.e2e.serializers.JacksonDeserializer;
import org.eclipse.edc.test.e2e.serializers.JacksonSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.validation.constraints.NotNull;

import static java.lang.String.format;
import static java.time.Duration.ZERO;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.test.system.utils.PolicyFixtures.inForceDatePolicy;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;
import static org.mockserver.verify.VerificationTimes.atLeast;
import static org.mockserver.verify.VerificationTimes.never;

@KafkaIntegrationTest
class EndToEndKafkaTransferTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String KAFKA_SERVER = "localhost:9092";
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private static final String SINK_HTTP_PATH = "/api/service";
    private static final String SOURCE_TOPIC = "source_topic";
    private static final String SINK_TOPIC = "sink_topic";
    private static final int EVENT_DESTINATION_PORT = getFreePort();
    private static final JsonNode JSON_MESSAGE = sampleMessage();
    private static final AtomicInteger MESSAGE_COUNTER = new AtomicInteger();

    private static final EndToEndTransferParticipant CONSUMER = EndToEndTransferParticipant.Builder.newInstance()
            .name("consumer")
            .id("urn:connector:consumer")
            .build();
    private static final EndToEndTransferParticipant PROVIDER = EndToEndTransferParticipant.Builder.newInstance()
            .name("provider")
            .id("urn:connector:provider")
            .build();

    @RegisterExtension
    static EdcRuntimeExtension consumerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane",
            "consumer-control-plane",
            CONSUMER.controlPlaneConfiguration()
    );

    @RegisterExtension
    static EdcRuntimeExtension providerDataPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:data-plane",
            "provider-data-plane",
            PROVIDER.dataPlaneConfiguration()
    );

    @RegisterExtension
    static EdcRuntimeExtension providerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane",
            "provider-control-plane",
            PROVIDER.controlPlaneConfiguration()
    );

    @BeforeAll
    public static void setUp() {
        startKafkaProducer();
    }

    @Test
    void kafkaToHttpTransfer() throws JsonProcessingException {
        var destinationServer = startClientAndServer(EVENT_DESTINATION_PORT);
        var request = request()
                .withMethod(HttpMethod.POST.name())
                .withPath(SINK_HTTP_PATH)
                .withBody(OBJECT_MAPPER.writeValueAsBytes(JSON_MESSAGE));
        destinationServer.when(request).respond(response());
        PROVIDER.registerDataPlane(Set.of("Kafka"), Set.of("HttpData"));

        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, kafkaSourceProperty());

        var transferProcessId = CONSUMER.requestAsset(PROVIDER, assetId, noPrivateProperty(), httpSink());

        await().atMost(TIMEOUT).untilAsserted(() -> {
            destinationServer.verify(request, atLeast(1));
        });

        await().atMost(TIMEOUT).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(TransferProcessStates.valueOf(state)).isGreaterThanOrEqualTo(TERMINATED);
        });

        destinationServer.clear(request)
                .when(request).respond(response());
        await().pollDelay(5, SECONDS).atMost(TIMEOUT).untilAsserted(() -> {
            destinationServer.verify(request, never());
        });

        stopQuietly(destinationServer);
    }

    @Test
    void kafkaToKafkaTransfer() {
        try (var consumer = createKafkaConsumer()) {
            consumer.subscribe(List.of(SINK_TOPIC));

            PROVIDER.registerDataPlane(Set.of("Kafka"), Set.of("Kafka"));

            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, kafkaSourceProperty());

            var transferProcessId = CONSUMER.requestAsset(PROVIDER, assetId, noPrivateProperty(), kafkaSink());
            await().atMost(TIMEOUT).untilAsserted(() -> {
                var records = consumer.poll(ZERO);
                assertThat(records.isEmpty()).isFalse();
                records.records(SINK_TOPIC).forEach(record -> assertThat(record.value()).isEqualTo(JSON_MESSAGE));
            });

            await().atMost(TIMEOUT).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(TransferProcessStates.valueOf(state)).isGreaterThanOrEqualTo(TERMINATED);
            });

            consumer.poll(ZERO);
            await().pollDelay(5, SECONDS).atMost(TIMEOUT).untilAsserted(() -> {
                var recordsFound = consumer.poll(Duration.ofSeconds(1)).records(SINK_TOPIC);
                assertThat(recordsFound).isEmpty();
            });
        }
    }

    private static Consumer<String, JsonNode> createKafkaConsumer() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVER);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "runner");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }

    private static Producer<String, JsonNode> createKafkaProducer() {
        var props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVER);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    private static void startKafkaProducer() {
        var producer = createKafkaProducer();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                () -> producer.send(new ProducerRecord<>(SOURCE_TOPIC, String.valueOf(MESSAGE_COUNTER.getAndIncrement()), JSON_MESSAGE)),
                0, 100, MILLISECONDS);
    }

    private void createResourcesOnProvider(String assetId, Map<String, Object> dataAddressProperties) {
        PROVIDER.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
        var policy = inForceDatePolicy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s");
        var policyDefinition = PROVIDER.createPolicyDefinition(policy);
        PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), policyDefinition, policyDefinition);
    }

    private static JsonObject httpSink() {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "properties", Json.createObjectBuilder()
                        .add(EDC_NAMESPACE + "name", "data")
                        .add(EDC_NAMESPACE + "baseUrl", format("http://localhost:%s", EVENT_DESTINATION_PORT))
                        .add(EDC_NAMESPACE + "path", SINK_HTTP_PATH)
                        .build())
                .build();
    }

    @NotNull
    private static JsonObject kafkaSink() {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "Kafka")
                .add(EDC_NAMESPACE + "properties", Json.createObjectBuilder()
                        .add(EDC_NAMESPACE + "topic", SINK_TOPIC)
                        .add(EDC_NAMESPACE + kafkaProperty("bootstrap.servers"), KAFKA_SERVER)
                        .build())
                .build();
    }

    @NotNull
    private static Map<String, Object> kafkaSourceProperty() {
        return Map.of(
                "name", "data",
                "type", "Kafka",
                "topic", SOURCE_TOPIC,
                kafkaProperty("bootstrap.servers"), KAFKA_SERVER,
                kafkaProperty("max.poll.records"), "100"
        );
    }

    private static JsonNode sampleMessage() {
        var node = OBJECT_MAPPER.createObjectNode();
        node.put("foo", "bar");
        return node;
    }

    private static String kafkaProperty(String property) {
        return "kafka." + property;
    }

    private static JsonObject noPrivateProperty() {
        return Json.createObjectBuilder().build();
    }
}
