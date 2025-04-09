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
     * Register a json object validator for a specific JsonLD @type. Validators are <em>composed</em>, that means adding multiple
     * validators for the same type will simply execute them in sequence and return the compounded result.
     *
     * @param type      the JsonLD @type string.
     * @param validator the validator to be executed.
     */
    void register(String type, Validator<JsonObject> validator);

    /**
     * Choose the correct validators and executes it on the input object. If multiple validators exist for a type, <em>all</em>
     * of them are executed and their results merged.
     *
     * @param type  the JsonLD @type string.
     * @param input the input object.
     * @return the result of the validation.
     */
    ValidationResult validate(String type, JsonObject input);

}
