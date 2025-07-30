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
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.resource.ClasspathSchemaLoader;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static com.networknt.schema.SpecVersion.VersionFlag.V201909;

public class ManagementApiSchemaValidatorProvider {
    
    private JsonSchemaFactory schemaFactory;
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
                    .map(ValidationMessage::getMessage)
                    .map(msg -> Violation.violation(msg, null))
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
            provider.schemaFactory = JsonSchemaFactory.getInstance(V201909, builder ->
                    builder.schemaLoaders(loader -> loader.add(new ClasspathSchemaLoader()))
                            .schemaMappers(schemaMappers -> prefixMappings.forEach(schemaMappers::mapPrefix)));
            return provider;
        }

    }

}
