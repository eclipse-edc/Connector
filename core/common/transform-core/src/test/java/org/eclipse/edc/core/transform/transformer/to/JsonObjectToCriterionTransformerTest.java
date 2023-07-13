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

package org.eclipse.edc.core.transform.transformer.to;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.core.transform.transformer.TestInput.getExpanded;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_RIGHT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERATOR;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class JsonObjectToCriterionTransformerTest {

    private final JsonObjectToCriterionTransformer transformer = new JsonObjectToCriterionTransformer();
    private final JsonValueToGenericTypeTransformer genericTypeTransformer = new JsonValueToGenericTypeTransformer(createObjectMapper());

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        when(context.transform(any(JsonValue.class), eq(Object.class))).thenAnswer(a -> genericTypeTransformer.transform(a.getArgument(0), context));
        var json = Json.createObjectBuilder()
                .add(JsonLdKeywords.TYPE, CRITERION_TYPE)
                .add(CRITERION_OPERAND_LEFT, "foo")
                .add(CRITERION_OPERAND_RIGHT, "bar")
                .add(CRITERION_OPERATOR, "=")
                .build();

        var crit = transformer.transform(getExpanded(json), context);
        assertThat(crit).isNotNull();
        assertThat(crit.getOperator()).isEqualTo("=");
        assertThat(crit.getOperandLeft()).isEqualTo("foo");
        assertThat(crit.getOperandRight()).isEqualTo("bar");
    }
}
