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

package org.eclipse.edc.validator.spi;

import jakarta.json.JsonObject;

/**
 * Registry service for {@link JsonObject} validators
 */
public interface JsonObjectValidatorRegistry {

    /**
     * Register a json object validator for a specific JsonLD @type
     *
     * @param type the JsonLD @type string.
     * @param validator the validator to be executed.
     */
    void register(String type, Validator<JsonObject> validator);

    /**
     * Choose the correct validator and executes it on the input object
     *
     * @param type the JsonLD @type string.
     * @param input the input object.
     * @return the result of the validation.
     */
    ValidationResult validate(String type, JsonObject input);

}
