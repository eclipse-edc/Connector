/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.catalog.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.catalog.model.CatalogRequestDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

public class CatalogRequestDtoToQuerySpecTransformer implements DtoTransformer<CatalogRequestDto, QuerySpec> {
    @Override
    public Class<CatalogRequestDto> getInputType() {
        return CatalogRequestDto.class;
    }

    @Override
    public Class<QuerySpec> getOutputType() {
        return QuerySpec.class;
    }

    @Override
    public @Nullable QuerySpec transform(@Nullable CatalogRequestDto requestDto, @NotNull TransformerContext context) {

        var spec = QuerySpec.Builder.newInstance();
        if (requestDto == null) {
            context.reportProblem("Input DTO is null");
            return null;
        }

        spec.sortField(requestDto.getSortField())
                .sortOrder(requestDto.getSortOrder())
                .limit(requestDto.getLimit())
                .offset(requestDto.getOffset());


        var criteria = requestDto.getFilter().stream()
                .map(dto -> context.transform(dto, Criterion.class))
                .collect(Collectors.toList());

        spec.filter(criteria);

        return spec.build();
    }
}
