/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.web.jersey.providers.jsonld;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.eclipse.edc.web.spi.validation.SchemaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JsonObjectMessageBodyReaderTest {

    private static final String EXPECTED_TYPE = "TestType";
    private static final String SCHEMA_VERSION = "v1";
    private static final String TYPE_CONTEXT = "test";

    private final JsonLd jsonLd = mock();
    private final TypeManager typeManager = mock();
    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper();

    private JsonObjectMessageBodyReader reader;

    @BeforeEach
    void setUp() {
        reader = new JsonObjectMessageBodyReader(jsonLd, typeManager, TYPE_CONTEXT, validatorRegistry);
        when(typeManager.getMapper(TYPE_CONTEXT)).thenReturn(objectMapper);
    }

    @Test
    void isReadable_shouldReturnTrue_forJsonObjectType() {
        assertThat(reader.isReadable(JsonObject.class, JsonObject.class, new Annotation[0], MediaType.APPLICATION_JSON_TYPE)).isTrue();
    }

    @Test
    void isReadable_shouldReturnFalse_forOtherType() {
        assertThat(reader.isReadable(String.class, String.class, new Annotation[0], MediaType.APPLICATION_JSON_TYPE)).isFalse();
    }

    @Test
    void readFrom_shouldReturnNull_whenBodyIsEmpty() throws IOException {
        var result = reader.readFrom(JsonObject.class, JsonObject.class, noAnnotations(), MediaType.APPLICATION_JSON_TYPE, null, emptyStream());

        assertThat(result).isNull();
        verifyNoInteractions(jsonLd, validatorRegistry);
    }

    @Test
    void readFrom_shouldExpandAndReturn_whenNoSchemaTypeAnnotation() throws IOException {
        var expanded = Json.createObjectBuilder().add("expanded-key", "value").build();
        when(jsonLd.expand(any())).thenReturn(Result.success(expanded));

        var result = reader.readFrom(JsonObject.class, JsonObject.class, noAnnotations(), MediaType.APPLICATION_JSON_TYPE, null, toStream("{\"key\":\"value\"}"));

        assertThat(result).isEqualTo(expanded);
        verify(jsonLd).expand(any());
        verifyNoInteractions(validatorRegistry);
    }

    @Test
    void readFrom_shouldValidateAndExpand_whenSchemaTypeAnnotationPresent() throws IOException {
        var input = Json.createObjectBuilder().add("@type", EXPECTED_TYPE).add("key", "value").build();
        var expanded = Json.createObjectBuilder().add("expanded-key", "value").build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(jsonLd.expand(any())).thenReturn(Result.success(expanded));

        var result = reader.readFrom(JsonObject.class, JsonObject.class, schemaTypeAnnotation(), MediaType.APPLICATION_JSON_TYPE, null, toStream(input.toString()));

        assertThat(result).isEqualTo(expanded);
        verify(validatorRegistry).validate(eq(SCHEMA_VERSION + ":" + EXPECTED_TYPE), any());
        verify(jsonLd).expand(any());
    }

    @Test
    void readFrom_shouldThrowInvalidRequest_whenTypePropertyMissing() {
        assertThatThrownBy(() ->
                reader.readFrom(JsonObject.class, JsonObject.class, schemaTypeAnnotation(), MediaType.APPLICATION_JSON_TYPE, null, toStream("{\"key\":\"value\"}")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("@type");

        verifyNoInteractions(validatorRegistry, jsonLd);
    }

    @Test
    void readFrom_shouldThrowInvalidRequest_whenTypeMismatch() {
        var input = Json.createObjectBuilder().add("@type", "WrongType").build();

        assertThatThrownBy(() ->
                reader.readFrom(JsonObject.class, JsonObject.class, schemaTypeAnnotation(), MediaType.APPLICATION_JSON_TYPE, null, toStream(input.toString())))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("WrongType");

        verifyNoInteractions(validatorRegistry, jsonLd);
    }

    @Test
    void readFrom_shouldThrowValidationFailure_whenValidationFails() {
        var input = Json.createObjectBuilder().add("@type", EXPECTED_TYPE).build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("invalid", "field")));

        assertThatThrownBy(() ->
                reader.readFrom(JsonObject.class, JsonObject.class, schemaTypeAnnotation(), MediaType.APPLICATION_JSON_TYPE, null, toStream(input.toString())))
                .isInstanceOf(ValidationFailureException.class);

        verify(validatorRegistry).validate(eq(SCHEMA_VERSION + ":" + EXPECTED_TYPE), any());
        verifyNoInteractions(jsonLd);
    }

    @Test
    void readFrom_shouldThrowInvalidRequest_whenExpansionFails() {
        var input = Json.createObjectBuilder().add("@type", EXPECTED_TYPE).build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(jsonLd.expand(any())).thenReturn(Result.failure("expansion error"));

        assertThatThrownBy(() ->
                reader.readFrom(JsonObject.class, JsonObject.class, schemaTypeAnnotation(), MediaType.APPLICATION_JSON_TYPE, null, toStream(input.toString())))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("expansion error");
    }

    @Test
    void readFrom_shouldAcceptAnyOfMultipleTypes_whenSchemaTypeHasMultipleValues() throws IOException {
        var input = Json.createObjectBuilder().add("@type", "TypeB").build();
        var expanded = Json.createObjectBuilder().add("expanded-key", "value").build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(jsonLd.expand(any())).thenReturn(Result.success(expanded));

        var result = reader.readFrom(JsonObject.class, JsonObject.class, multiTypeAnnotation(), MediaType.APPLICATION_JSON_TYPE, null, toStream(input.toString()));

        assertThat(result).isEqualTo(expanded);
        verify(validatorRegistry).validate(eq(SCHEMA_VERSION + ":TypeB"), any());
    }

    private Annotation[] noAnnotations() {
        return new Annotation[0];
    }

    private Annotation[] schemaTypeAnnotation() {
        return new Annotation[]{ schemaType(new String[]{ EXPECTED_TYPE }, SCHEMA_VERSION) };
    }

    private Annotation[] multiTypeAnnotation() {
        return new Annotation[]{ schemaType(new String[]{ "TypeA", "TypeB" }, SCHEMA_VERSION) };
    }

    private SchemaType schemaType(String[] value, String version) {
        return new SchemaType() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return SchemaType.class;
            }

            @Override
            public String[] value() {
                return value;
            }

            @Override
            public String version() {
                return version;
            }
        };
    }

    private InputStream toStream(String json) {
        return new ByteArrayInputStream(json.getBytes());
    }

    private InputStream emptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }
}
