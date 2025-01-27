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

package org.eclipse.edc.transform.transformer.edc.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromCriterionTransformerTest {

    private final ObjectMapper mapper = mock(ObjectMapper.class);
    private final TransformerContext context = mock(TransformerContext.class);
    private final TypeManager typeManager = mock();
    private JsonObjectFromCriterionTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromCriterionTransformer(Json.createBuilderFactory(Map.of()), typeManager, "test");
        when(typeManager.getMapper("test")).thenReturn(mapper);
    }

    @Test
    void transform() {

        var criterion = Criterion.Builder.newInstance()
                .operandLeft("field")
                .operator("=")
                .operandRight("value")
                .build();

        when(mapper.convertValue(eq(criterion.getOperandLeft()), eq(JsonValue.class))).thenReturn(Json.createValue(criterion.getOperandLeft().toString()));
        when(mapper.convertValue(eq(criterion.getOperandRight()), eq(JsonValue.class))).thenReturn(Json.createValue(criterion.getOperandRight().toString()));

        var jsonObject = transformer.transform(criterion, context);

        assertThat(jsonObject).isNotNull();

        assertThat(jsonObject.getJsonString(Criterion.CRITERION_OPERAND_LEFT).getString()).isEqualTo(criterion.getOperandLeft());
        assertThat(jsonObject.getJsonString(Criterion.CRITERION_OPERATOR).getString()).isEqualTo(criterion.getOperator());
        assertThat(jsonObject.getJsonString(Criterion.CRITERION_OPERAND_RIGHT).getString()).isEqualTo(criterion.getOperandRight());

    }

    @Test
    void transform_reportErrors() {

        var criterion = Criterion.Builder.newInstance()
                .operandLeft("field")
                .operator("=")
                .operandRight("value")
                .build();

        when(context.problem()).thenReturn(new ProblemBuilder(context));

        when(mapper.convertValue(eq(criterion.getOperandLeft()), eq(JsonValue.class))).thenThrow(new IllegalArgumentException());
        when(mapper.convertValue(eq(criterion.getOperandRight()), eq(JsonValue.class))).thenThrow(new IllegalArgumentException());

        var jsonObject = transformer.transform(criterion, context);

        assertThat(jsonObject).isNotNull();
        verify(context, times(2)).reportProblem(anyString());

    }

}
