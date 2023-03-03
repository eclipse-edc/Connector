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
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.test.e2e.annotations.KafkaIntegrationTest;
import org.eclipse.edc.test.e2e.serializers.JacksonDeserializer;
import org.eclipse.edc.test.e2e.serializers.JacksonSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.validation.constraints.NotNull;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.stop.Stop.stopQuietly;

@KafkaIntegrationTest
class KafkaTransferTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String KAFKA_SERVER = "localhost:9092";

    protected final Duration timeout = Duration.ofSeconds(60);

    private static final String SINK_HTTP_PATH = "/api/service";
    private static final String SOURCE_TOPIC = "source_topic";
    private static final String SINK_TOPIC = "sink_topic";
    private static final Participant CONSUMER = new Participant("consumer", "urn:connector:consumer");
    private static final Participant PROVIDER = new Participant("provider", "urn:connector:provider");
    private static final int EVENT_DESTINATION_PORT = getFreePort();
    private static final JsonNode JSON_MESSAGE = sampleMessage();

    private static ClientAndServer eventDestination;
    private static ScheduledExecutorService executor;

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

    private static final AtomicInteger MESSAGE_COUNTER = new AtomicInteger();

    @BeforeAll
    public static void setUp() {
        eventDestination = startClientAndServer(EVENT_DESTINATION_PORT);
        startKafkaProducer();
    }

    @AfterAll
    public static void tearDown() {
        stopQuietly(eventDestination);
        executor.shutdown();
    }

    /**
     * Reset mock server internal state after every test.
     */
    @AfterEach
    public void resetMockServer() {
        eventDestination.reset();
    }

    @Test
    void kafkaToHttpTransfer() throws JsonProcessingException {
        PROVIDER.registerDataPlane();

        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), UUID.randomUUID().toString(), kafkaSourceProperty());
        var agreementId = negotiateContractForAssetId(assetId);
        initiateTransfer(agreementId, assetId, httpSink());

        var requestDefinition = HttpRequest.request()
                .withMethod(HttpMethod.POST.name())
                .withPath(SINK_HTTP_PATH)
                .withBody(OBJECT_MAPPER.writeValueAsBytes(JSON_MESSAGE));
        await().atMost(timeout).untilAsserted(() -> eventDestination.verify(requestDefinition, VerificationTimes.atLeast(1)));
    }

    @Test
    void kafkaToKafkaTransfer() {
        try (var consumer = createKafkaConsumer()) {
            consumer.subscribe(List.of(SINK_TOPIC));

            PROVIDER.registerDataPlane();

            var assetId = UUID.randomUUID().toString();
            createResourcesOnProvider(assetId, noConstraintPolicy(), UUID.randomUUID().toString(), kafkaSourceProperty());
            var agreementId = negotiateContractForAssetId(assetId);
            initiateTransfer(agreementId, assetId, kafkaSink());

            await().atMost(timeout).untilAsserted(() -> {
                var records = consumer.poll(Duration.ZERO);
                assertThat(records.isEmpty()).isFalse();
                records.records(SINK_TOPIC).forEach(record -> assertThat(record.value()).isEqualTo(JSON_MESSAGE));
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
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(
                () -> producer.send(new ProducerRecord<>(SOURCE_TOPIC, String.valueOf(MESSAGE_COUNTER.getAndIncrement()), JSON_MESSAGE)),
                0, 100, TimeUnit.MILLISECONDS);
    }

    private void initiateTransfer(String contractAgreementId, String assetId, DataAddress destination) {
        var transferProcessId = CONSUMER.dataRequest(UUID.randomUUID().toString(), contractAgreementId, assetId, PROVIDER, destination);

        await().atMost(timeout).pollDelay(Duration.ofSeconds(1)).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
        });
    }

    private String negotiateContractForAssetId(String assetId) {
        var catalog = CONSUMER.getCatalog(PROVIDER);

        var contractOffer = catalog
                .getContractOffers()
                .stream()
                .filter(o -> o.getAssetId().equals(assetId))
                .findFirst()
                .orElseThrow();

        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractOffer);
        assertThat(contractAgreementId).isNotEmpty();
        return contractAgreementId;
    }

    private void createResourcesOnProvider(String assetId, PolicyDefinition contractPolicy, String definitionId, Map<String, String> dataAddressProperties) {
        PROVIDER.createAsset(assetId, dataAddressProperties);
        var accessPolicy = noConstraintPolicy();
        PROVIDER.createPolicy(accessPolicy);
        PROVIDER.createPolicy(contractPolicy);
        PROVIDER.createContractDefinition(assetId, definitionId, accessPolicy.getUid(), contractPolicy.getUid(), 31536000L);
    }

    @NotNull
    private HttpDataAddress httpSink() {
        return HttpDataAddress.Builder.newInstance()
                .name("data")
                .baseUrl(format("http://localhost:%s", EVENT_DESTINATION_PORT))
                .path(SINK_HTTP_PATH)
                .build();
    }

    @NotNull
    private DataAddress kafkaSink() {
        return DataAddress.Builder.newInstance()
                .property("type", "Kafka")
                .property("topic", SINK_TOPIC)
                .property(kafkaProperty("bootstrap.servers"), KAFKA_SERVER)
                .build();
    }

    @NotNull
    private Map<String, String> kafkaSourceProperty() {
        return Map.of(
                "name", "data",
                "type", "Kafka",
                "topic", SOURCE_TOPIC,
                kafkaProperty("bootstrap.servers"), KAFKA_SERVER,
                kafkaProperty("max.poll.records"), "100"
        );
    }

    private PolicyDefinition noConstraintPolicy() {
        return PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance()
                        .permission(Permission.Builder.newInstance()
                                .action(Action.Builder.newInstance().type("USE").build())
                                .build())
                        .type(PolicyType.SET)
                        .build())
                .build();
    }

    private static JsonNode sampleMessage() {
        var node = OBJECT_MAPPER.createObjectNode();
        node.put("foo", "bar");
        return node;
    }

    private static String kafkaProperty(String property) {
        return "kafka." + property;
    }
}
