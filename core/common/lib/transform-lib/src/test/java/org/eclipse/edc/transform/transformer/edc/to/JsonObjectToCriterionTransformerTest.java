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

package org.eclipse.edc.transform.transformer.edc.to;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_RIGHT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERATOR;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_TYPE;
import static org.eclipse.edc.transform.transformer.TestInput.getExpanded;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class JsonObjectToCriterionTransformerTest {
    private final TypeManager typeManager = mock();

    private final JsonObjectToCriterionTransformer transformer = new JsonObjectToCriterionTransformer();
    private final JsonValueToGenericTypeTransformer genericTypeTransformer = new JsonValueToGenericTypeTransformer(typeManager, "test");
    
    @BeforeEach
    void setup() {
        when(typeManager.getMapper("test")).thenReturn(JacksonJsonLd.createObjectMapper());
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        when(context.transform(any(JsonValue.class), eq(Object.class))).thenAnswer(a -> genericTypeTransformer.transform(a.getArgument(0), context));
        var json = Json.createObjectBuilder()
                .add(JsonLdKeywords.TYPE, CRITERION_TYPE)
                .add(CRITERION_OPERAND_LEFT, "foo")
                .add(CRITERION_OPERATOR, "=")
                .add(CRITERION_OPERAND_RIGHT, "bar")
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();
        assertThat(result.getOperandLeft()).isEqualTo("foo");
        assertThat(result.getOperator()).isEqualTo("=");
        assertThat(result.getOperandRight()).isEqualTo("bar");
    }

    @Test
    void transform_shouldConsiderRightAsList_whenOperatorIsIn() {
        var context = mock(TransformerContext.class);
        when(context.transform(any(JsonValue.class), eq(Object.class))).thenAnswer(a -> genericTypeTransformer.transform(a.getArgument(0), context));
        var json = Json.createObjectBuilder()
                .add(JsonLdKeywords.TYPE, CRITERION_TYPE)
                .add(CRITERION_OPERAND_LEFT, "foo")
                .add(CRITERION_OPERATOR, "in")
                .add(CRITERION_OPERAND_RIGHT, Json.createArrayBuilder().add("bar").add("baz"))
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();
        assertThat(result.getOperandLeft()).isEqualTo("foo");
        assertThat(result.getOperator()).isEqualTo("in");
        assertThat(result.getOperandRight()).isInstanceOf(List.class).asList().containsExactly("bar", "baz");
    }

    @Test
    void transform_rightOperandIsNumber() {
        var context = mock(TransformerContext.class);
        when(context.transform(any(JsonValue.class), eq(Object.class))).thenAnswer(a -> genericTypeTransformer.transform(a.getArgument(0), context));
        when(context.problem()).thenReturn(mock());
        var json = Json.createObjectBuilder()
                .add(JsonLdKeywords.TYPE, CRITERION_TYPE)
                .add(CRITERION_OPERAND_LEFT, "foo")
                .add(CRITERION_OPERATOR, "=")
                .add(CRITERION_OPERAND_RIGHT, 42)
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();
        assertThat(result.getOperandRight()).satisfies(obj -> {
            assertThat(obj).isInstanceOf(Double.class);
            // must cast to engage the AbstractDoubleAssert
            assertThat((Double) obj).isEqualTo(42);
        });
    }
}
