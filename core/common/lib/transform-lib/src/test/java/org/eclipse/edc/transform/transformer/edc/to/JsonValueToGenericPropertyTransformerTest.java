/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.transform.transformer.edc.to;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.transformer.TestInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JsonValueToGenericPropertyTransformerTest {

    private final TypeManager typeManager = mock();
    private final ObjectMapper mapper = mock();
    private final TransformerContext context = mock();
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private JsonValueToGenericTypeTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonValueToGenericTypeTransformer(typeManager, "test");
        when(typeManager.getMapper("test")).thenReturn(mapper);

    }

    @Test
    void transform_jsonObjectWithoutValueField_returnObject() throws JsonProcessingException {
        var jsonObject = jsonFactory.createObjectBuilder()
                .add(CONTEXT, JsonObject.EMPTY_JSON_OBJECT)
                .add(ODRL_SCHEMA + "someProperty", "someProperty")
                .build();

        var object = new Object();

        when(mapper.readValue(anyString(), eq(Object.class))).thenReturn(object);

        var result = transformer.transform(TestInput.getExpanded(jsonObject), context);

        assertThat(result).isNotNull().isInstanceOf(Object.class).isEqualTo(object);

        verifyNoInteractions(context);
        verify(mapper).readValue(anyString(), eq(Object.class));
    }

    @Test
    void transform_jsonObjectWithValueField_returnObject() throws JsonProcessingException {
        var value = "value";
        var jsonObject = jsonFactory.createObjectBuilder()
                .add(VALUE, value)
                .build();

        when(mapper.readValue(anyString(), eq(Object.class))).thenReturn(value);

        // note: do not expand is it is already in expanded form
        var result = transformer.transform(jsonObject, context);

        assertThat(result).isNotNull().isInstanceOf(String.class).hasToString(value);

        verifyNoInteractions(context);
    }

    @Test
    void transform_jsonObject_errorMappingToJavaType_reportProblem() throws JsonProcessingException {
        var jsonObject = jsonFactory.createObjectBuilder()
                .add(CONTEXT, JsonObject.EMPTY_JSON_OBJECT)
                .add(ODRL_SCHEMA + "property", "someProperty")
                .build();

        when(mapper.readValue(anyString(), eq(Object.class))).thenThrow(JsonProcessingException.class);

        var result = transformer.transform(TestInput.getExpanded(jsonObject), context);

        assertThat(result).isNull();
        verify(context, times(1)).reportProblem(anyString());
        verify(mapper).readValue(anyString(), eq(Object.class));
    }

    @Test
    void transform_jsonArray_returnList() throws JsonProcessingException {
        var value = "value";
        // include a string, int and object to transform in the array
        var jsonArray = jsonFactory.createArrayBuilder()
                .add(jsonFactory.createObjectBuilder().add(VALUE, value).build())
                .add(jsonFactory.createObjectBuilder().add(VALUE, 1).build())
                .add(jsonFactory.createObjectBuilder().add(ODRL_SCHEMA + "someProperty", "test").build())
                .build();
        when(mapper.readValue(anyString(), eq(Object.class))).thenReturn(value);

        var result = transformer.transform(jsonArray, context);

        assertThat(result).isNotNull().isInstanceOf(List.class);
        assertThat((List<?>) result).hasSize(3);
        assertThat(((List<?>) result).get(0)).isInstanceOf(String.class).hasToString(value);

        verifyNoInteractions(context);
        verify(mapper).readValue(anyString(), eq(Object.class));
    }

    @Test
    void transform_jsonString_returnString() {
        var value = "value";
        var jsonString = Json.createValue(value);

        var result = transformer.transform(jsonString, context);

        assertThat(result).isNotNull().isInstanceOf(String.class).hasToString(value);

        verifyNoInteractions(context);
    }

    @Test
    void transform_jsonNumber_returnNumber() {
        var value = 42.0;
        var jsonNumber = Json.createValue(value);

        var result = transformer.transform(jsonNumber, context);

        assertThat(result).isNotNull().isInstanceOf(Double.class).matches(o -> ((Double) o) == value);

        verifyNoInteractions(context);
    }
}
