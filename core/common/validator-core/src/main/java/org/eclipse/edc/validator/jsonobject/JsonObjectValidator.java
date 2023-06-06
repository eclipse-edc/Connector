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

package org.eclipse.edc.validator.jsonobject;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.eclipse.edc.validator.jsonobject.JsonLdPath.path;

/**
 * The {@link JsonObject} {@link Validator} implementation.
 * It supports validation on nested objects and definition in a fluent way.
 */
public class JsonObjectValidator implements Validator<JsonObject> {

    private final List<Validator<JsonObject>> validators = new ArrayList<>();
    private final JsonLdPath path;
    private final Navigator navigator;

    public static JsonObjectValidator newValidator() {
        return new JsonObjectValidator();
    }

    private JsonObjectValidator() {
        this(path(), new RootObjectNavigator());
    }

    protected JsonObjectValidator(JsonLdPath path, Navigator navigator) {
        this.path = path;
        this.navigator = navigator;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        var violations = navigator.extract(input, path)
                .flatMap(target -> this.validators.stream().map(validator -> validator.validate(target)))
                .filter(ValidationResult::failed)
                .flatMap(it -> it.getFailure().getViolations().stream())
                .toList();

        if (violations.isEmpty()) {
            return ValidationResult.success();
        } else {
            return ValidationResult.failure(violations);
        }
    }

    public JsonObjectValidator verify(Function<JsonLdPath, Validator<JsonObject>> validatorProvider) {
        var validator = validatorProvider.apply(path);
        this.validators.add(validator);
        return this;
    }

    public JsonObjectValidator verify(String fieldName, Function<JsonLdPath, Validator<JsonObject>> validatorProvider) {
        var newPath = path.append(fieldName);
        var validator = validatorProvider.apply(newPath);
        this.validators.add(validator);
        return this;
    }

    public JsonObjectValidator verifyObject(String fieldName, UnaryOperator<JsonObjectValidator> provider) {
        var newPath = path.append(fieldName);
        this.validators.add(provider.apply(new JsonObjectValidator(newPath, new NestedObjectNavigator())));
        return this;
    }

    private static class RootObjectNavigator implements Navigator {

        @Override
        public Stream<JsonObject> extract(JsonObject object, JsonLdPath path) {
            return Stream.of(object);
        }
    }

    private static class NestedObjectNavigator implements Navigator {
        @Override
        public Stream<JsonObject> extract(JsonObject o, JsonLdPath p) {
            var array = o.getJsonArray(p.last());

            if (array == null) {
                return Stream.empty();
            } else {
                return Stream.of(array.getJsonObject(0));
            }
        }
    }

    interface Navigator {
        Stream<JsonObject> extract(JsonObject object, JsonLdPath path);
    }
}
