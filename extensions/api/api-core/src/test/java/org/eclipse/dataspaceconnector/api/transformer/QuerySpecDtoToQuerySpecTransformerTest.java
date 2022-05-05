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

package org.eclipse.dataspaceconnector.api.transformer;

import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

}