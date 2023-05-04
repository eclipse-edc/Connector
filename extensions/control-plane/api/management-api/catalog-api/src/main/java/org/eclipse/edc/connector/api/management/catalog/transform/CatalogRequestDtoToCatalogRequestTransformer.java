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

package org.eclipse.edc.connector.api.management.catalog.transform;

import org.eclipse.edc.catalog.spi.CatalogRequest;
import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CatalogRequestDtoToCatalogRequestTransformer implements TypeTransformer<CatalogRequestDto, CatalogRequest> {
    @Override
    public Class<CatalogRequestDto> getInputType() {
        return CatalogRequestDto.class;
    }

    @Override
    public Class<CatalogRequest> getOutputType() {
        return CatalogRequest.class;
    }

    @Override
    public @Nullable CatalogRequest transform(@NotNull CatalogRequestDto dto, @NotNull TransformerContext context) {
        return CatalogRequest.Builder.newInstance()
                .providerUrl(dto.getProviderUrl())
                .protocol(dto.getProtocol())
                .querySpec(context.transform(dto.getQuerySpec(), QuerySpec.class))
                .build();
    }
}
