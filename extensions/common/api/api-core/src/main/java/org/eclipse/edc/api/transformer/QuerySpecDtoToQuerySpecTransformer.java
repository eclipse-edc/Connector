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

import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuerySpecDtoToQuerySpecTransformer implements DtoTransformer<QuerySpecDto, QuerySpec> {

    @Override
    public Class<QuerySpecDto> getInputType() {
        return QuerySpecDto.class;
    }

    @Override
    public Class<QuerySpec> getOutputType() {
        return QuerySpec.class;
    }

    @Override
    public @Nullable QuerySpec transform(@NotNull QuerySpecDto query, @NotNull TransformerContext context) {
        var builder = QuerySpec.Builder.newInstance()
                .limit(query.getLimit())
                .offset(query.getOffset())
                .sortField(query.getSortField())
                .sortOrder(query.getSortOrder());

        query.getFilterExpression()
                .forEach(criterionDto -> {
                    var result = context.transform(criterionDto, Criterion.class);
                    if (result == null) {
                        context.reportProblem("CriterionDto can not be transformed: " + criterionDto);
                    } else {
                        builder.filter(result);
                    }
                });

        if (context.hasProblems()) {
            return null;
        }

        return builder.build();
    }

}
