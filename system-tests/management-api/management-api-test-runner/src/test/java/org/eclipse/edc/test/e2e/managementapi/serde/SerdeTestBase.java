/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.test.e2e.managementapi.serde;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.MANAGEMENT_API_SCOPE;

abstract class SerdeTestBase {

    protected String jsonLdScope() {
        return MANAGEMENT_API_SCOPE;
    }

    protected List<String> transformerScope() {
        return List.of(MANAGEMENT_API_CONTEXT);
    }

    protected abstract String jsonLdContext();

    protected abstract String schemaVersion();

    protected boolean strictSchema() {
        return false;
    }

    protected JsonObject serialize(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validator, JsonLd jsonLd, Object object) {
        var registry = forContext(typeTransformerRegistry, transformerScope());

        var result = registry.transform(object, JsonObject.class).orElseThrow(failure -> new RuntimeException());
        var compacted = jsonLd.compact(result, jsonLdScope()).orElseThrow(failure -> new RuntimeException(failure.getFailureDetail()));
        validate(validator, compacted);
        return compacted;
    }

    private void validate(JsonObjectValidatorRegistry validator, JsonObject compacted) {
        var type = compacted.getJsonString(TYPE) != null ? compacted.getString(TYPE) : null;
        if (type != null) {
            var validationResult = validator.validate(schemaVersion() + ":" + type, compacted);
            if (!validationResult.succeeded()) {
                throw new RuntimeException("Validation failed: " + validationResult.getFailureDetail());
            }
        }
    }

    protected void verifySerde(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd, JsonObject inputObject, Class<?> klass, Function<JsonObject, JsonObject> mapper) {
        var registry = forContext(typeTransformerRegistry, transformerScope());

        validate(validatorRegistry, inputObject);

        // Expand the input
        var expanded = jsonLd.expand(inputObject).orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        // transform the expanded into the input klass type
        var result = registry.transform(expanded, klass).orElseThrow(failure -> new RuntimeException(failure.getFailureDetail()));
        // transform the klass type instance into JsonObject
        var object = registry.transform(result, JsonObject.class).orElseThrow(failure -> new RuntimeException());

        // Compact the result
        var compactResult = jsonLd.compact(object, jsonLdScope());

        // checks that the compacted == inputObject
        AbstractResultAssert.assertThat(compactResult).isSucceeded().satisfies(compacted -> {
            var mapped = Optional.ofNullable(mapper).map(m -> m.apply(compacted)).orElse(compacted);
            assertThat(mapped).isEqualTo(inputObject);
        });
    }

    protected ValidationResult validateWithResult(JsonObjectValidatorRegistry validator, JsonObject compacted) {
        var type = compacted.getJsonString(TYPE) != null ? compacted.getString(TYPE) : null;
        if (type != null) {
            return validator.validate(schemaVersion() + ":" + type, compacted);
        } else {
            throw new RuntimeException("Validation failed: no type");
        }
    }

    protected <T> T deserialize(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validator, JsonLd jsonLd, JsonObject inputObject, Class<T> klass) {
        validate(validator, inputObject);
        var registry = forContext(typeTransformerRegistry, transformerScope());

        var expanded = jsonLd.expand(inputObject).orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        // checks that the type is correctly expanded to the EDC namespace
        assertThat(expanded.getJsonArray(TYPE)).first().satisfies(t -> {
            assertThat(((JsonString) t).getString()).startsWith(EDC_NAMESPACE);
        });

        return registry.transform(expanded, klass).orElseThrow(failure -> new RuntimeException());
    }

    private TypeTransformerRegistry forContext(TypeTransformerRegistry typeTransformerRegistry, List<String> context) {
        for (String ctx : context) {
            typeTransformerRegistry = typeTransformerRegistry.forContext(ctx);
        }
        return typeTransformerRegistry;
    }
}
