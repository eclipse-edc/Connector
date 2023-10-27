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

import org.eclipse.edc.dataaddress.kafka.spi.KafkaDataAddressSchema;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;

import static org.eclipse.edc.validator.dataaddress.kafka.KafkaDataAddressValidatorExtension.NAME;

@Extension(NAME)
public class KafkaDataAddressValidatorExtension implements ServiceExtension {
    public static final String NAME = "DataAddress Kafka Validator";

    @Inject
    private DataAddressValidatorRegistry dataAddressValidatorRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var validator = new KafkaDataAddressValidator();
        dataAddressValidatorRegistry.registerSourceValidator(KafkaDataAddressSchema.KAFKA_TYPE, validator);
        dataAddressValidatorRegistry.registerDestinationValidator(KafkaDataAddressSchema.KAFKA_TYPE, validator);
    }
}
