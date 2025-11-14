/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class ManagementApiSchemaValidatorProvider {

    private SchemaRegistry schemaFactory;
    private Supplier<ObjectMapper> objectMapperSupplier;

    private ManagementApiSchemaValidatorProvider() {
    }

    public Validator<JsonObject> validatorFor(String schema) {
        var schemaValidator = schemaFactory.getSchema(SchemaLocation.of(schema));
        return (input) -> {
            var node = objectMapperSupplier.get().convertValue(input, JsonNode.class);
            var response = schemaValidator.validate(node);
            if (response.isEmpty()) {
                return ValidationResult.success();
            }

            var violations = response.stream()
                    .map(error -> Violation.violation(error.getMessage(), error.getInstanceLocation().toString()))
                    .toList();

            return ValidationResult.failure(violations);
        };
    }


    public static class Builder {

        private final ManagementApiSchemaValidatorProvider provider;

        private final Map<String, String> prefixMappings = new HashMap<>();

        private Builder() {
            provider = new ManagementApiSchemaValidatorProvider();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder objectMapper(Supplier<ObjectMapper> objectMapperSupplier) {
            provider.objectMapperSupplier = objectMapperSupplier;
            return this;
        }

        public Builder prefixMapping(String prefix, String location) {
            Objects.requireNonNull(prefix, "Prefix must not be null");
            Objects.requireNonNull(location, "Location must not be null");
            prefixMappings.put(prefix, location);
            return this;
        }

        public ManagementApiSchemaValidatorProvider build() {
            Objects.requireNonNull(provider.objectMapperSupplier);
            provider.schemaFactory = SchemaRegistry.withDialect(Dialects.getDraft201909(), builder ->
                    builder.schemaIdResolvers(schemaIdResolvers -> prefixMappings.forEach(schemaIdResolvers::mapPrefix)));
            return provider;
        }

    }

}
