/*
 *  Copyright (c) 2024 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.validator.jsonobject.validators;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Optional;

import static java.lang.String.format;

/**
 * Verify that a @value is present but deprecated.
 */
public class LogDeprecatedValue implements Validator<JsonObject> {
    public final String deprecatedLog;
    private final JsonLdPath path;
    private final Monitor monitor;

    public LogDeprecatedValue(JsonLdPath path, String deprecatedLog, Monitor monitor) {
        this.path = path;
        this.monitor = monitor;
        this.deprecatedLog = deprecatedLog;
    }

    public LogDeprecatedValue(JsonLdPath path, String deprecatedType, String attributeToUse, Monitor monitor) {
        this(path, format("The attribute %s has been deprecated in type %s, please use %s",
                path.last(), deprecatedType, attributeToUse), monitor);
    }
    @Override
    public ValidationResult validate(JsonObject input) {
        return Optional.ofNullable(input.getJsonArray(path.last()))
                .filter(it -> !it.isEmpty())
                .map(it -> {
                    monitor.warning(deprecatedLog);
                    return ValidationResult.success();
                }).orElseGet(ValidationResult::success);
    }
}
