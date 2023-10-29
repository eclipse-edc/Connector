/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.validator.spi;

import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Registry service for DataAddress validation
 */
public interface DataAddressValidatorRegistry {

    /**
     * Register a source DataAddress object validator for a specific DataAddress type
     *
     * @param type the DataAddress type string.
     * @param validator the validator to be executed.
     */
    void registerSourceValidator(String type, Validator<DataAddress> validator);

    /**
     * Register a destination DataAddress object validator for a specific DataAddress type
     *
     * @param type the DataAddress type string.
     * @param validator the validator to be executed.
     */
    void registerDestinationValidator(String type, Validator<DataAddress> validator);

    /**
     * Validate a source data address
     *
     * @param dataAddress the source data address.
     * @return the validation result.
     */
    ValidationResult validateSource(DataAddress dataAddress);

    /**
     * Validate a destination data address
     *
     * @param dataAddress the destination data address.
     * @return the validation result.
     */
    ValidationResult validateDestination(DataAddress dataAddress);
}
