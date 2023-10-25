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

package org.eclipse.edc.connector.core.validator;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DataAddressValidatorRegistryImpl implements DataAddressValidatorRegistry {

    private final Map<String, Validator<DataAddress>> sourceValidators = new HashMap<>();
    private final Map<String, Validator<DataAddress>> destinationValidators = new HashMap<>();
    private final Monitor monitor;

    public DataAddressValidatorRegistryImpl(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void registerSourceValidator(String type, Validator<DataAddress> validator) {
        sourceValidators.put(type, validator);
    }

    @Override
    public void registerDestinationValidator(String type, Validator<DataAddress> validator) {
        destinationValidators.put(type, validator);
    }

    @Override
    public ValidationResult validateSource(DataAddress dataAddress) {
        return sourceValidators.getOrDefault(dataAddress.getType(), d -> warning("source")).validate(dataAddress);
    }

    @Override
    public ValidationResult validateDestination(DataAddress dataAddress) {
        return destinationValidators.getOrDefault(dataAddress.getType(), d -> warning("destination")).validate(dataAddress);
    }

    @NotNull
    private ValidationResult warning(String type) {
        monitor.warning("No %s DataAddress validator has been registered, please register one as it is strongly recommended.".formatted(type));
        return ValidationResult.success();
    }
}
