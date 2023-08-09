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

package org.eclipse.edc.dataplane.kafka.pipeline;

import org.eclipse.edc.dataplane.kafka.config.KafkaPropertiesFactory;
import org.eclipse.edc.dataplane.kafka.schema.KafkaDataAddressSchema;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Map;
import java.util.Properties;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaDataSourceFactoryTest {

    private final KafkaPropertiesFactory propertiesFactory = mock(KafkaPropertiesFactory.class);

    private KafkaDataSourceFactory factory;

    @BeforeEach
    public void setUp() {
        factory = new KafkaDataSourceFactory(mock(Monitor.class), propertiesFactory, mock(Clock.class));
    }

    @Test
    void verifyCanHandle() {
        assertThat(factory.canHandle(createRequest("kafka", emptyMap()))).isTrue();
        assertThat(factory.canHandle(createRequest("KaFka", emptyMap()))).isTrue();
        assertThat(factory.canHandle(createRequest("kafkax", emptyMap()))).isFalse();
    }

    @Test
    void verifyValidateSuccess() {
        var request = createRequest(KafkaDataAddressSchema.KAFKA_TYPE, Map.of(KafkaDataAddressSchema.TOPIC, "test"));

        when(propertiesFactory.getConsumerProperties(request.getSourceDataAddress().getProperties()))
                .thenReturn(Result.success(mock(Properties.class)));

        var result = factory.validateRequest(request);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void verifyValidateReturnsFailedResult_ifMissingTopicProperty() {
        var request = createRequest(KafkaDataAddressSchema.KAFKA_TYPE, emptyMap());

        when(propertiesFactory.getConsumerProperties(request.getSourceDataAddress().getProperties()))
                .thenReturn(Result.success(mock(Properties.class)));

        var result = factory.validateRequest(request);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).contains("topic");
    }

    @Test
    void verifyValidateReturnsFailedResult_ifKafkaPropertiesFactoryFails() {
        var errorMsg = "test-error";
        var request = createRequest(KafkaDataAddressSchema.KAFKA_TYPE, Map.of(KafkaDataAddressSchema.TOPIC, "test"));

        when(propertiesFactory.getConsumerProperties(request.getSourceDataAddress().getProperties()))
                .thenReturn(Result.failure(errorMsg));

        var result = factory.validateRequest(request);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).contains(errorMsg);
    }

    @Test
    void verifyCreateSourceThrows_ifMissingTopicProperty() {
        var request = createRequest(KafkaDataAddressSchema.KAFKA_TYPE, emptyMap());

        when(propertiesFactory.getConsumerProperties(request.getSourceDataAddress().getProperties()))
                .thenReturn(Result.success(mock(Properties.class)));

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> factory.createSource(request));
    }

    @Test
    void verifyCreateSourceThrows_ifKafkaPropertiesFactoryFails() {
        var errorMsg = "test-error";
        var request = createRequest(KafkaDataAddressSchema.KAFKA_TYPE, Map.of(KafkaDataAddressSchema.TOPIC, "test"));

        when(propertiesFactory.getConsumerProperties(request.getSourceDataAddress().getProperties()))
                .thenReturn(Result.failure(errorMsg));

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> factory.createSource(request));
    }

    private DataFlowRequest createRequest(String sourceType, Map<String, Object> sourceProperties) {
        return DataFlowRequest.Builder.newInstance()
                .id("id")
                .processId("processId")
                .destinationDataAddress(DataAddress.Builder.newInstance().type("notused").build())
                .sourceDataAddress(DataAddress.Builder.newInstance()
                        .type(sourceType)
                        .properties(sourceProperties)
                        .build())
                .build();
    }
}
