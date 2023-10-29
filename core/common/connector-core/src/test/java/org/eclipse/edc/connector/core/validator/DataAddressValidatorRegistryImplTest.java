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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataAddressValidatorRegistryImplTest {

    private final Monitor monitor = mock();
    private final DataAddressValidatorRegistry validator = new DataAddressValidatorRegistryImpl(monitor);

    @Nested
    class Source {
        @Test
        void shouldCallRegisteredValidator() {
            Validator<DataAddress> typeValidator = mock();
            when(typeValidator.validate(any())).thenReturn(ValidationResult.success());
            validator.registerSourceValidator("type", typeValidator);
            var dataAddress = DataAddress.Builder.newInstance()
                    .property("type", "type")
                    .build();

            var result = validator.validateSource(dataAddress);

            assertThat(result).isSucceeded();
            verify(typeValidator).validate(dataAddress);
        }

        @Test
        void shouldSucceedWithWarning_whenTypeIsNotRegistered() {
            var dataAddress = DataAddress.Builder.newInstance()
                    .property("type", "not-registered")
                    .build();

            var result = validator.validateSource(dataAddress);

            assertThat(result).isSucceeded();
            verify(monitor).warning(anyString());
        }
    }

    @Nested
    class Destination {
        @Test
        void shouldCallRegisteredValidator() {
            Validator<DataAddress> typeValidator = mock();
            when(typeValidator.validate(any())).thenReturn(ValidationResult.success());
            validator.registerDestinationValidator("type", typeValidator);
            var dataAddress = DataAddress.Builder.newInstance()
                    .property("type", "type")
                    .build();

            var result = validator.validateDestination(dataAddress);

            assertThat(result).isSucceeded();
            verify(typeValidator).validate(dataAddress);
        }

        @Test
        void shouldSucceedWithWarning_whenTypeIsNotRegistered() {
            var dataAddress = DataAddress.Builder.newInstance()
                    .property("type", "not-registered")
                    .build();

            var result = validator.validateDestination(dataAddress);

            assertThat(result).isSucceeded();
            verify(monitor).warning(anyString());
        }
    }

}
