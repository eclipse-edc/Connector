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

package org.eclipse.edc.dataplane.kafka.pipeline.validation;

import org.eclipse.edc.connector.dataplane.util.validation.CompositeValidationRule;
import org.eclipse.edc.connector.dataplane.util.validation.EmptyValueValidationRule;
import org.eclipse.edc.connector.dataplane.util.validation.ValidationRule;
import org.eclipse.edc.dataplane.kafka.config.KafkaPropertiesFactory;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.dataplane.kafka.schema.KafkaDataAddressSchema.TOPIC;

public class KafkaSourceDataAddressValidationRule implements ValidationRule<DataAddress> {

    private final CompositeValidationRule<Map<String, String>> validationRule;

    public KafkaSourceDataAddressValidationRule(KafkaPropertiesFactory propertiesFactory) {
        this.validationRule = new CompositeValidationRule<>(
                List.of(
                        new EmptyValueValidationRule(TOPIC),
                        new ConsumerPropertiesValidationRule(propertiesFactory)
                )
        );
    }

    @Override
    public Result<Void> apply(DataAddress dataAddress) {
        return validationRule.apply(dataAddress.getProperties());
    }

    private static final class ConsumerPropertiesValidationRule implements ValidationRule<Map<String, String>> {

        private final KafkaPropertiesFactory propertiesFactory;

        private ConsumerPropertiesValidationRule(KafkaPropertiesFactory propertiesFactory) {
            this.propertiesFactory = propertiesFactory;
        }

        @Override
        public Result<Void> apply(Map<String, String> properties) {
            return propertiesFactory.getConsumerProperties(properties)
                    .compose(p -> Result.success());
        }
    }
}
