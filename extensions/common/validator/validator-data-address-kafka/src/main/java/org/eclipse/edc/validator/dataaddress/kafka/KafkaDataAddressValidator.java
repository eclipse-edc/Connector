/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.validator.dataaddress.kafka;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Objects;
import java.util.stream.Stream;

import static org.eclipse.edc.dataaddress.kafka.spi.KafkaDataAddressSchema.BOOTSTRAP_SERVERS;
import static org.eclipse.edc.dataaddress.kafka.spi.KafkaDataAddressSchema.TOPIC;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validator for Kafka DataAddress type
 */
public class KafkaDataAddressValidator implements Validator<DataAddress> {

    @Override
    public ValidationResult validate(DataAddress input) {
        var violations = Stream.of(TOPIC, BOOTSTRAP_SERVERS)
                .map(it -> {
                    var value = input.getStringProperty(it);
                    if (value == null || value.isBlank()) {
                        return violation("'%s' is a mandatory attribute".formatted(it), it, value);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        if (violations.isEmpty()) {
            return ValidationResult.success();
        }

        return ValidationResult.failure(violations);
    }

}
