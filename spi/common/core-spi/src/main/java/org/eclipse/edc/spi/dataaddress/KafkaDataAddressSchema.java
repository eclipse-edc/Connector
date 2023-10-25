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

package org.eclipse.edc.spi.dataaddress;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * Defines the schema of a DataAddress representing a Kafka endpoint.
 */
public interface KafkaDataAddressSchema {

    /**
     * The transfer type.
     */
    String KAFKA_TYPE = "Kafka";

    /**
     * The Kafka stream name.
     */
    String NAME = EDC_NAMESPACE + "name";

    /**
     * The Kafka topic.
     */
    String TOPIC = EDC_NAMESPACE + "topic";

    /**
     * The prefix for Kafka properties. These properties are passed to the Kafka which is removed. For example, a property named {@code kafka.key.deserializer} will
     * be passed to the Kafka client as {@code key.deserializer}.
     */
    String KAFKA_PROPERTIES_PREFIX = EDC_NAMESPACE + "kafka.";

    /**
     * The bootstrap.servers property
     */
    String BOOTSTRAP_SERVERS = KAFKA_PROPERTIES_PREFIX + "bootstrap.servers";

    /**
     * The duration of the consumer polling.
     * <p>
     * The value should be a ISO-8601 duration e.g. "PT10S" for 10 seconds.
     * This parameter is optional. Default value is 1s.
     *
     * @see java.time.Duration#parse(CharSequence) for ISO-8601 duration format
     */
    String POLL_DURATION = EDC_NAMESPACE + "pollDuration";

    /**
     * Maximum duration of the stream before it closes.
     * <p>
     * The value should be a ISO-8601 duration e.g. "PT10S" for 10 seconds.
     * This parameter is optional. If not provided, then the stream never ends.
     *
     * @see java.time.Duration#parse(CharSequence) for ISO-8601 duration format
     */
    String MAX_DURATION = EDC_NAMESPACE + "maxDuration";
}
