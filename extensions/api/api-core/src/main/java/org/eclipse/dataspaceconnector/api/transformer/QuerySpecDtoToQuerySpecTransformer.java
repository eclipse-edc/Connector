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
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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
    public @Nullable QuerySpec transform(@Nullable QuerySpecDto object, @NotNull TransformerContext context) {
        var sortOrder = Optional.of(object).map(QuerySpecDto::getSortOrder).map(SortOrder::valueOf).orElse(SortOrder.ASC);
        return QuerySpec.Builder.newInstance()
                .limit(object.getLimit())
                .offset(object.getOffset())
                .filter(object.getFilter())
                .sortField(object.getSortField())
                .sortOrder(sortOrder)
                .build();
    }
}
