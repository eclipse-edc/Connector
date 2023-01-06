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
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

        // use filter string
        builder.filter(query.getFilter());

        // overwrite with filter expression, if present
        if (!query.getFilterExpression().isEmpty()) {
            var result = transformFilter(query.getFilterExpression(), context);
            if (result.succeeded()) {
                builder.filter(result.getContent());
            } else {
                context.reportProblem(result.getFailureDetail());
                return null;
            }
        }
        return builder.build();
    }

    private Result<List<Criterion>> transformFilter(List<CriterionDto> filterExpression, TransformerContext context) {
        var transformed = filterExpression.stream()
                .map(dto -> context.transform(dto, Criterion.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return transformed.size() == filterExpression.size() ? Result.success(transformed) : Result.failure("Some elements could not be transformed");
    }
}
