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

package org.eclipse.edc.api.transformer;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonValue;
import org.eclipse.edc.core.transform.transformer.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERAND_RIGHT;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERATOR;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToCriterionDtoTransformerTest {

    private final JsonObjectToCriterionDtoTransformer transformer = new JsonObjectToCriterionDtoTransformer();
    private final JsonValueToGenericTypeTransformer genericTypeTransformer = new JsonValueToGenericTypeTransformer(createObjectMapper());
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void transform() {
        when(context.transform(any(JsonValue.class), eq(Object.class))).thenAnswer(a -> genericTypeTransformer.transform(a.getArgument(0), context));
        var json = createObjectBuilder()
                .add(JsonLdKeywords.TYPE, CRITERION_TYPE)
                .add(CRITERION_OPERAND_LEFT, value("foo"))
                .add(CRITERION_OPERAND_RIGHT, value("bar"))
                .add(CRITERION_OPERATOR, value("="))
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getOperator()).isEqualTo("=");
        assertThat(result.getOperandLeft()).isEqualTo("foo");
        assertThat(result.getOperandRight()).isEqualTo("bar");
    }

    @Test
    void transforms_shouldTransformRightOperandAsList_whenOperatorIsIn() {
        var context = mock(TransformerContext.class);
        when(context.transform(any(JsonValue.class), eq(Object.class))).thenAnswer(a -> genericTypeTransformer.transform(a.getArgument(0), context));
        var json = createObjectBuilder()
                .add(JsonLdKeywords.TYPE, CRITERION_TYPE)
                .add(CRITERION_OPERAND_LEFT, value("foo"))
                .add(CRITERION_OPERATOR, "in")
                .add(CRITERION_OPERAND_RIGHT, createArrayBuilder()
                        .add(createObjectBuilder().add(VALUE, "bar"))
                        .add(createObjectBuilder().add(VALUE, "baz"))
                )
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getOperator()).isEqualTo("in");
        assertThat(result.getOperandLeft()).isEqualTo("foo");
        assertThat(result.getOperandRight()).asList().containsExactly("bar", "baz");
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }
}
