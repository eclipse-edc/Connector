/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.validator.jsonobject.validators;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

/**
 * Verifies that after expansion all the properties are not prefixed with the configured prefixes for the runtime.
 */
public class MissingPrefixes implements Validator<JsonObject> {

    private final JsonLdPath path;

    private final Supplier<Set<String>> prefixesSupplier;

    public MissingPrefixes(JsonLdPath path, Supplier<Set<String>> prefixesSupplier) {
        this.path = path;
        this.prefixesSupplier = prefixesSupplier;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        var prefixes = prefixesSupplier.get();
        return Optional.ofNullable(input.getJsonArray(path.last()))
                .filter(it -> !it.isEmpty())
                .map(it -> it.getJsonObject(0))
                .or(() -> Optional.of(input))
                .map(it -> validateObject(it, path, prefixes))
                .orElseGet(ValidationResult::success);
    }

    private ValidationResult validateObject(JsonObject input, JsonLdPath path, Set<String> prefixes) {
        return input.entrySet().stream().map(entry -> validateField(entry.getKey(), entry.getValue(), path, prefixes))
                .reduce(ValidationResult::merge)
                .orElse(ValidationResult.success());
    }

    private ValidationResult validateArray(JsonArray array, JsonLdPath path, Set<String> prefixes) {
        return array.stream().filter(f -> f instanceof JsonObject)
                .map(JsonObject.class::cast)
                .map(object -> validateObject(object, path, prefixes))
                .reduce(ValidationResult::merge)
                .orElse(ValidationResult.success());
    }

    private ValidationResult validateField(String name, JsonValue value, JsonLdPath path, Set<String> prefixes) {
        return switch (name) {
            case TYPE -> validateTypeValue(value, path, prefixes);
            case ID -> validateIdValue(value, path, prefixes);
            default -> validateGenericField(name, value, path, prefixes);
        };

    }

    private ValidationResult validateTypeValue(JsonValue value, JsonLdPath path, Set<String> prefixes) {
        if (value instanceof JsonArray array) {
            return array.stream()
                    .filter(it -> it.getValueType() == JsonValue.ValueType.STRING)
                    .map(JsonString.class::cast)
                    .map(JsonString::getString)
                    .map(type -> validateType(type, path, prefixes))
                    .reduce(ValidationResult::merge)
                    .orElseGet(ValidationResult::success);
        } else if (value instanceof JsonString type) {
            return validateType(type.getString(), path, prefixes);
        } else {
            return ValidationResult.success();
        }
    }

    private ValidationResult validateType(String type, JsonLdPath path, Set<String> prefixes) {
        var msg = "Value of @type contains a prefix '%s' which was not expended correctly. Ensure to attach the namespace definition in the input JSON-LD.";
        return validate(type, path, prefixes, msg::formatted);
    }

    private ValidationResult validate(String input, JsonLdPath path, Set<String> prefixes, Function<String, String> formatter) {
        return Arrays.stream(input.split(":"))
                .findFirst()
                .map(prefix -> validatePrefix(prefix, path, prefixes, formatter))
                .orElseGet(ValidationResult::success);
    }

    private ValidationResult validatePrefix(String prefix, JsonLdPath path, Set<String> prefixes, Function<String, String> formatter) {
        if (prefixes.contains(prefix)) {
            var msg = formatter.apply(prefix);
            return ValidationResult.failure(Violation.violation(msg, path.toString()));
        } else {
            return ValidationResult.success();
        }
    }

    private ValidationResult validateId(String id, JsonLdPath path, Set<String> prefixes) {
        var msg = "Value of @id contains a prefix '%s' which was not expended correctly. Ensure to attach the namespace definition in the input JSON-LD.";
        return validate(id, path, prefixes, msg::formatted);
    }

    private ValidationResult validateIdValue(JsonValue value, JsonLdPath path, Set<String> prefixes) {
        if (value instanceof JsonString id) {
            return validateId(id.getString(), path, prefixes);
        } else {
            return ValidationResult.success();
        }
    }

    private ValidationResult validateGenericField(String name, JsonValue value, JsonLdPath path, Set<String> prefixes) {
        var newPath = path.append(name);
        var msg = "Property %s, contains a prefix '%s' which was not expended correctly. Ensure to attach the namespace definition in the input JSON-LD.";
        var result = validate(name, newPath, prefixes, (prefix) -> msg.formatted(name, prefix));

        if (result.failed()) {
            return result;
        } else {
            if (value instanceof JsonObject object) {
                return validateObject(object, newPath, prefixes);
            } else if (value instanceof JsonArray array) {
                return validateArray(array, newPath, prefixes);
            } else {
                return ValidationResult.success();
            }
        }
    }
}