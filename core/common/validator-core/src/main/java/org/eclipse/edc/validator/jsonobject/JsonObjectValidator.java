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
import jakarta.json.JsonString;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.validator.jsonobject.JsonLdPath.path;
import static org.eclipse.edc.validator.jsonobject.JsonWalkers.ARRAY_ITEMS;
import static org.eclipse.edc.validator.jsonobject.JsonWalkers.NESTED_OBJECT;
import static org.eclipse.edc.validator.jsonobject.JsonWalkers.ROOT_OBJECT;

/**
 * The {@link JsonObject} {@link Validator} implementation.
 * It supports validation on nested objects and definition in a fluent way.
 */
public class JsonObjectValidator implements Validator<JsonObject> {

    private final List<Validator<JsonObject>> validators = new ArrayList<>();
    private final JsonLdPath path;
    private final JsonWalker walker;

    public static JsonObjectValidator.Builder newValidator() {
        return JsonObjectValidator.Builder.newInstance(path(), ROOT_OBJECT);
    }

    protected JsonObjectValidator(JsonLdPath path, JsonWalker walker) {
        this.path = path;
        this.walker = walker;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        if (input == null) {
            return ValidationResult.failure(Violation.violation("input json is null", path.toString()));
        }

        var violations = walker.extract(input, path)
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

    public static class Builder {

        private final JsonObjectValidator validator;

        private Builder(JsonObjectValidator validator) {
            this.validator = validator;
        }

        public static Builder newInstance(JsonLdPath path, JsonWalker walker) {
            return new Builder(new JsonObjectValidator(path, walker));
        }

        public Builder verify(Function<JsonLdPath, Validator<JsonObject>> validatorProvider) {
            validator.validators.add(validatorProvider.apply(validator.path));
            return this;
        }

        public Builder verify(String fieldName, Function<JsonLdPath, Validator<JsonObject>> validatorProvider) {
            var newPath = validator.path.append(fieldName);
            validator.validators.add(validatorProvider.apply(newPath));
            return this;
        }

        public Builder verifyId(Function<JsonLdPath, Validator<JsonString>> idValidatorProvider) {
            var newPath = validator.path.append(ID);
            validator.validators.add(input -> idValidatorProvider.apply(newPath).validate(input.getJsonString(ID)));
            return this;
        }

        public Builder verifyObject(String fieldName, UnaryOperator<JsonObjectValidator.Builder> provider) {
            var newPath = validator.path.append(fieldName);
            var builder = JsonObjectValidator.Builder.newInstance(newPath, NESTED_OBJECT);
            validator.validators.add(provider.apply(builder).build());
            return this;
        }

        public Builder verifyArrayItem(String fieldName, UnaryOperator<JsonObjectValidator.Builder> provider) {
            var newPath = validator.path.append(fieldName);
            var builder = JsonObjectValidator.Builder.newInstance(newPath, ARRAY_ITEMS);
            validator.validators.add(provider.apply(builder).build());
            return this;
        }

        public JsonObjectValidator build() {
            return validator;
        }
    }
}
