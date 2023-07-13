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

package org.eclipse.edc.core.transform.transformer.from;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectFromQuerySpecTransformerTest {

    private JsonObjectFromQuerySpecTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromQuerySpecTransformer(Json.createBuilderFactory(Map.of()));
    }

    @Test
    void transform() {
        var querySpec = QuerySpec.Builder.newInstance()
                .limit(10)
                .offset(30)
                .sortField("field")
                .sortOrder(SortOrder.DESC)
                .filter(List.of(Criterion.Builder.newInstance().operator("=").operandLeft("test").build()))
                .build();

        var context = mock(TransformerContext.class);
        when(context.transform(isA(Criterion.class), eq(JsonObject.class))).thenReturn(Json.createObjectBuilder().build());
        var jsonObject = transformer.transform(querySpec, context);

        assertThat(jsonObject).isNotNull();

        assertThat(jsonObject.getInt(QuerySpec.EDC_QUERY_SPEC_LIMIT)).isEqualTo(querySpec.getLimit());
        assertThat(jsonObject.getInt(QuerySpec.EDC_QUERY_SPEC_OFFSET)).isEqualTo(querySpec.getOffset());
        assertThat(jsonObject.getJsonString(QuerySpec.EDC_QUERY_SPEC_SORT_FIELD).getString()).isEqualTo(querySpec.getSortField());
        assertThat(jsonObject.getJsonString(QuerySpec.EDC_QUERY_SPEC_SORT_ORDER).getString()).isEqualTo(querySpec.getSortOrder().toString());
        assertThat(jsonObject.get(QuerySpec.EDC_QUERY_SPEC_FILTER_EXPRESSION))
                .isNotNull()
                .isInstanceOf(JsonArray.class)
                .matches(v -> v.asJsonArray().size() == 1);

    }

}
