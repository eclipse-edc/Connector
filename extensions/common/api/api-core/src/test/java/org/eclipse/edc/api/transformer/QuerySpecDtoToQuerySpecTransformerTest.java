/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuerySpecDtoToQuerySpecTransformerTest {

    private final QuerySpecDtoToQuerySpecTransformer transformer = new QuerySpecDtoToQuerySpecTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var querySpecDto = QuerySpecDto.Builder.newInstance()
                .offset(10)
                .limit(20)
                .filter("field=value")
                .sortOrder(SortOrder.DESC)
                .sortField("field")
                .build();

        var spec = transformer.transform(querySpecDto, context);

        assertThat(spec.getOffset()).isEqualTo(10);
        assertThat(spec.getLimit()).isEqualTo(20);
        assertThat(spec.getFilterExpression()).hasSize(1)
                .containsExactly(new Criterion("field", "=", "value"));
        assertThat(spec.getSortOrder()).isEqualTo(SortOrder.DESC);
        assertThat(spec.getSortField()).isEqualTo("field");
    }

    @Test
    void transform_defaultValues() {
        var context = mock(TransformerContext.class);
        var querySpecDto = QuerySpecDto.Builder.newInstance().build();

        var spec = transformer.transform(querySpecDto, context);

        assertThat(spec.getOffset()).isEqualTo(0);
        assertThat(spec.getLimit()).isEqualTo(50);
        assertThat(spec.getFilterExpression()).hasSize(0);
        assertThat(spec.getSortOrder()).isEqualTo(SortOrder.ASC);
        assertThat(spec.getSortField()).isNull();
    }

    @Test
    void transform_shouldReturnNull_whenCriterionNotTransforable() {
        var context = mock(TransformerContext.class);
        when(context.transform(isA(CriterionDto.class), eq(Criterion.class)))
                .thenReturn(null);

        var querySpecDto = QuerySpecDto.Builder.newInstance()
                .filterExpression(List.of(CriterionDto.from("foo", "=", "bar")))
                .filter("bar < baz")
                .build();

        var spec = transformer.transform(querySpecDto, context);
        assertThat(spec).isNull();

        verify(context).reportProblem(eq("Some elements could not be transformed"));
    }

    @Test
    void transform_shouldFallbackToFilter() {
        var context = mock(TransformerContext.class);
        var expected = new Criterion("bar", "<", "baz");
        when(context.transform(isA(CriterionDto.class), eq(Criterion.class)))
                .thenReturn(expected);

        var querySpecDto = QuerySpecDto.Builder.newInstance()
                .filter("bar < baz")
                .build();

        var spec = transformer.transform(querySpecDto, context);
        assertThat(spec).isNotNull();
        assertThat(spec.getFilterExpression()).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsOnly(expected);
    }

}
