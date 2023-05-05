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

import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.catalog.spi.CatalogRequest;
import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogRequestDtoToCatalogRequestTransformerTest {

    private final CatalogRequestDtoToCatalogRequestTransformer transformer = new CatalogRequestDtoToCatalogRequestTransformer();
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(CatalogRequestDto.class);
        assertThat(transformer.getOutputType()).isEqualTo(CatalogRequest.class);
    }

    @Test
    void transform() {
        var querySpecDto = QuerySpecDto.Builder.newInstance().build();
        var querySpec = QuerySpec.Builder.newInstance().build();
        var dto = CatalogRequestDto.Builder.newInstance()
                .providerUrl("http://provider/url")
                .protocol("protocol")
                .querySpec(querySpecDto)
                .build();
        when(context.transform(any(), eq(QuerySpec.class))).thenReturn(querySpec);

        var result = transformer.transform(dto, context);

        assertThat(result).isNotNull();
        assertThat(result.getProtocol()).isEqualTo("protocol");
        assertThat(result.getProviderUrl()).isEqualTo("http://provider/url");
        assertThat(result.getQuerySpec()).isSameAs(querySpec);
    }
}
