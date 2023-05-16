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

package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.transformer.to.TestInput.getExpanded;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_FILTER_EXPRESSION;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_LIMIT;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_OFFSET;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_SORT_FIELD;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_SORT_ORDER;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.spi.query.SortOrder.DESC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToQuerySpecTransformerTest {

    private final JsonObjectToQuerySpecTransformer transformer = new JsonObjectToQuerySpecTransformer();
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getOutputType()).isEqualTo(QuerySpec.class);
    }

    @Test
    void transform() {
        var filterExpressionJson = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().build())
                .build();
        
        var criterion = Criterion.Builder.newInstance().operandLeft("test").operator("=").build();
        when(context.transform(any(), eq(Criterion.class))).thenReturn(criterion);
        var json = Json.createObjectBuilder()
                .add(TYPE, EDC_QUERY_SPEC_TYPE)
                .add(EDC_QUERY_SPEC_OFFSET, 10)
                .add(EDC_QUERY_SPEC_LIMIT, 20)
                .add(EDC_QUERY_SPEC_FILTER_EXPRESSION, filterExpressionJson)
                .add(EDC_QUERY_SPEC_SORT_ORDER, "DESC")
                .add(EDC_QUERY_SPEC_SORT_FIELD, "fieldName")
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(10);
        assertThat(result.getLimit()).isEqualTo(20);
        assertThat(result.getFilterExpression()).containsExactly(criterion);
        assertThat(result.getSortOrder()).isEqualTo(DESC);
        assertThat(result.getSortField()).isEqualTo("fieldName");
        verify(context).transform(any(), eq(Criterion.class));
    }

}
