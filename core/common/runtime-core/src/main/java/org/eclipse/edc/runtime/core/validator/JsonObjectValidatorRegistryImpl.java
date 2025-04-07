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

package org.eclipse.edc.runtime.core.validator;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.HashMap;
import java.util.Map;

public class JsonObjectValidatorRegistryImpl implements JsonObjectValidatorRegistry {

    private final Map<String, Validator<JsonObject>> validators = new HashMap<>();

    @Override
    public void register(String type, Validator<JsonObject> validator) {
        this.validators.put(type, validator);
    }

    @Override
    public ValidationResult validate(String type, JsonObject input) {
        return validators.getOrDefault(type, (i) -> ValidationResult.success())
                .validate(input);
    }
}
