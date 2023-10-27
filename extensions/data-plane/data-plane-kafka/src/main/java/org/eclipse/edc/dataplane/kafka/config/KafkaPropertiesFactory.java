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

package org.eclipse.edc.dataplane.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.edc.spi.result.Result;

import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.eclipse.edc.dataaddress.kafka.spi.KafkaDataAddressSchema.KAFKA_PROPERTIES_PREFIX;

public class KafkaPropertiesFactory {

    public Result<Properties> getConsumerProperties(Map<String, Object> properties) {
        return getCommonProperties(properties)
                .map(props -> {
                    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
                    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
                    return props;
                });
    }

    public Result<Properties> getProducerProperties(Map<String, Object> properties) {
        return getCommonProperties(properties)
                .map(props -> {
                    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
                    return props;
                });
    }

    private Result<Properties> getCommonProperties(Map<String, Object> properties) {
        var props = new Properties();
        properties.entrySet().stream()
                .filter(e -> e.getKey().startsWith(KAFKA_PROPERTIES_PREFIX))
                .forEach(entry -> props.put(entry.getKey().replaceFirst(Pattern.quote(KAFKA_PROPERTIES_PREFIX), ""), entry.getValue()));
        return Result.success(props);
    }
}
