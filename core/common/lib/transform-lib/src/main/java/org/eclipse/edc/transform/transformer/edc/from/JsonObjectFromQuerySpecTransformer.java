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

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromQuerySpecTransformer extends AbstractJsonLdTransformer<QuerySpec, JsonObject> {
    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromQuerySpecTransformer(JsonBuilderFactory jsonFactory) {
        super(QuerySpec.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull QuerySpec querySpec, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(TYPE, QuerySpec.EDC_QUERY_SPEC_TYPE);
        builder.add(QuerySpec.EDC_QUERY_SPEC_LIMIT, querySpec.getLimit());
        builder.add(QuerySpec.EDC_QUERY_SPEC_OFFSET, querySpec.getOffset());
        builder.add(QuerySpec.EDC_QUERY_SPEC_SORT_ORDER, querySpec.getSortOrder().toString());

        if (querySpec.getSortField() != null) {
            builder.add(QuerySpec.EDC_QUERY_SPEC_SORT_FIELD, querySpec.getSortField());
        }

        var filterExpressions = querySpec.getFilterExpression().stream()
                .map(expression -> context.transform(expression, JsonObject.class))
                .collect(jsonFactory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();

        builder.add(QuerySpec.EDC_QUERY_SPEC_FILTER_EXPRESSION, filterExpressions);

        return builder.build();
    }
}
