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

    protected JsonObjectValidator(JsonLdPath path, JsonWalker walker) {
        this.path = path;
        this.walker = walker;
    }

    public static JsonObjectValidator.Builder newValidator() {
        return JsonObjectValidator.Builder.newInstance(path(), ROOT_OBJECT);
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        if (input == null) {
            return ValidationResult.failure(Violation.violation("input json is null", path.toString()));
        }

        return walker.extract(input, path)
                .flatMap(target -> this.validators.stream().map(validator -> validator.validate(target)))
                .reduce(ValidationResult::merge)
                .orElse(ValidationResult.success());
    }

    public static class Builder {

        private final JsonObjectValidator validator;

        private Builder(JsonObjectValidator validator) {
            this.validator = validator;
        }

        public static Builder newInstance(JsonLdPath path, JsonWalker walker) {
            return new Builder(new JsonObjectValidator(path, walker));
        }

        /**
         * Add a validator on the root object level.
         *
         * @param provider the validator provider.
         * @return the builder.
         */
        public Builder verify(Function<JsonLdPath, Validator<JsonObject>> provider) {
            validator.validators.add(provider.apply(validator.path));
            return this;
        }

        /**
         * Add a validator on a specific field.
         *
         * @param fieldName the name of the field to be validated.
         * @param provider  the validator provider.
         * @return the builder.
         */
        public Builder verify(String fieldName, Function<JsonLdPath, Validator<JsonObject>> provider) {
            var newPath = validator.path.append(fieldName);
            validator.validators.add(provider.apply(newPath));
            return this;
        }

        /**
         * Add a validator on the @id field.
         *
         * @param provider the validator provider.
         * @return the builder.
         */
        public Builder verifyId(Function<JsonLdPath, Validator<JsonString>> provider) {
            var newPath = validator.path.append(ID);
            validator.validators.add(input -> provider.apply(newPath).validate(input.getJsonString(ID)));
            return this;
        }

        /**
         * Add a validator on a specific nested object.
         *
         * @param fieldName the name of the nested object to be validated.
         * @param provider  the validator provider.
         * @return the builder.
         */
        public Builder verifyObject(String fieldName, UnaryOperator<JsonObjectValidator.Builder> provider) {
            var newPath = validator.path.append(fieldName);
            var builder = JsonObjectValidator.Builder.newInstance(newPath, NESTED_OBJECT);
            validator.validators.add(provider.apply(builder).build());
            return this;
        }

        /**
         * Add a validator on a specific nested array.
         *
         * @param fieldName the name of the nested array to be validated.
         * @param provider  the validator provider.
         * @return the builder.
         */
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
