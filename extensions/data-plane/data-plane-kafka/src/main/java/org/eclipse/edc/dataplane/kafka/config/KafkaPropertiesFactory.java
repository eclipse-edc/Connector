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

import static java.lang.String.format;
import static java.util.Map.entry;
import static org.eclipse.edc.dataplane.kafka.schema.KafkaDataAddressSchema.KAFKA_PROPERTIES_PREFIX;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class KafkaPropertiesFactory {

    private static final String BOOTSTRAP_SERVERS_CONFIG = "bootstrap.servers";

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
        // TODO applied quick fix to handle json-ld edc namespace. to be fixed with https://github.com/eclipse-edc/Connector/issues/3005
        var props = new Properties();
        properties.entrySet().stream()
                .map(e -> entry(e.getKey().replace(EDC_NAMESPACE, ""), e.getValue()))
                .filter(e -> e.getKey().startsWith(KAFKA_PROPERTIES_PREFIX))
                .forEach(entry -> props.put(entry.getKey().replaceFirst(Pattern.quote(KAFKA_PROPERTIES_PREFIX), ""), entry.getValue()));
        if (!props.containsKey(BOOTSTRAP_SERVERS_CONFIG)) {
            return Result.failure(format("Missing `%s` config", BOOTSTRAP_SERVERS_CONFIG));
        }
        return Result.success(props);
    }
}
