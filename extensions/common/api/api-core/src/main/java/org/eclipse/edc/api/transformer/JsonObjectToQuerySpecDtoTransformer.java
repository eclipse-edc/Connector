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

import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.api.model.QuerySpecDto.EDC_QUERY_SPEC_FILTER_EXPRESSION;
import static org.eclipse.edc.api.model.QuerySpecDto.EDC_QUERY_SPEC_LIMIT;
import static org.eclipse.edc.api.model.QuerySpecDto.EDC_QUERY_SPEC_OFFSET;
import static org.eclipse.edc.api.model.QuerySpecDto.EDC_QUERY_SPEC_SORT_FIELD;
import static org.eclipse.edc.api.model.QuerySpecDto.EDC_QUERY_SPEC_SORT_ORDER;

public class JsonObjectToQuerySpecDtoTransformer extends AbstractJsonLdTransformer<JsonObject, QuerySpecDto> {

    public JsonObjectToQuerySpecDtoTransformer() {
        super(JsonObject.class, QuerySpecDto.class);
    }

    @Override
    public @Nullable QuerySpecDto transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var builder = QuerySpecDto.Builder.newInstance();

        visitProperties(input, key -> {
            switch (key) {
                case EDC_QUERY_SPEC_OFFSET:
                    return v -> builder.offset(transformInt(v, context));
                case EDC_QUERY_SPEC_LIMIT:
                    return v -> builder.limit(transformInt(v, context));
                case EDC_QUERY_SPEC_FILTER_EXPRESSION:
                    return v -> builder.filterExpression(transformArray(v, CriterionDto.class, context));
                case EDC_QUERY_SPEC_SORT_ORDER:
                    return v -> builder.sortOrder(SortOrder.valueOf(transformString(v, context)));
                case EDC_QUERY_SPEC_SORT_FIELD:
                    return v -> builder.sortField(transformString(v, context));
                default:
                    return doNothing();
            }
        });

        return builder.build();
    }

}
