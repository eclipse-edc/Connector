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

package org.eclipse.edc.jsonld.spi.transformer;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractJsonLdTransformerReturnObjectTest {

    public static final String TEST_PROPERTY = "testProperty";
    private AbstractJsonLdTransformer<Object, Object> transformer;
    private JsonBuilderFactory jsonFactory;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new AbstractJsonLdTransformer<>(Object.class, Object.class) {

            @Nullable
            @Override
            public Object transform(@NotNull Object o, @NotNull TransformerContext context) {
                return null;
            }
        };

        jsonFactory = Json.createBuilderFactory(Map.of());
        context = mock(TransformerContext.class);
        when(context.problem()).thenReturn(new ProblemBuilder(context));
    }

    @Test
    void verify_returnFromJsonObject() {
        var object = jsonFactory.createObjectBuilder().build();

        var result = transformer.returnMandatoryJsonObject(object, context, TEST_PROPERTY);

        assertThat(result).isSameAs(object);
        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void verify_returnFromNull() {
        var result = transformer.returnMandatoryJsonObject(null, context, TEST_PROPERTY);

        assertThat(result).isNull();
        verify(context, times(1)).reportProblem(eq(format("Property '%s' was null", TEST_PROPERTY)));
    }

    @Test
    void verify_returnFromNullNotMandatory() {
        var result = transformer.returnJsonObject(null, context, TEST_PROPERTY, false);

        assertThat(result).isNull();
        verify(context, never());
    }

    @Test
    void verify_returnFromInvalidType() {
        var value = jsonFactory.createObjectBuilder().add("test", "test").build().get("test");

        var result = transformer.returnMandatoryJsonObject(value, context, TEST_PROPERTY);

        assertThat(result).isNull();
        verify(context, times(1)).reportProblem(eq("Property 'testProperty' must be OBJECT or ARRAY but was: \"test\""));
    }

    @Test
    void verify_returnFromJsonArray() {
        var object = jsonFactory.createObjectBuilder().build();
        var array = jsonFactory.createArrayBuilder().add(object).build();

        var result = transformer.returnMandatoryJsonObject(array, context, TEST_PROPERTY);

        assertThat(result).isSameAs(object);
        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void verify_returnFromEmptyJsonArray() {
        var array = jsonFactory.createArrayBuilder().build();

        var result = transformer.returnMandatoryJsonObject(array, context, TEST_PROPERTY);

        assertThat(result).isNull();
        verify(context, times(1)).reportProblem(eq(format("Property '%s' contains an empty array", TEST_PROPERTY)));
    }

    @Test
    void verify_nodeJsonValue() {

        var object = jsonFactory.createObjectBuilder().add("name", "value").build();
        var array = jsonFactory.createObjectBuilder().add(VALUE, object).build();

        var result = transformer.nodeJsonValue(array);

        assertThat(result).isInstanceOf(JsonObject.class)
                .isEqualTo(object);
    }

}
