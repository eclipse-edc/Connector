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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_PROTOCOL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_PROVIDER_URL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_QUERY_SPEC;
import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToCatalogRequestDtoTransformerTest {

    private final JsonObjectToCatalogRequestDtoTransformer transformer = new JsonObjectToCatalogRequestDtoTransformer();
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getOutputType()).isEqualTo(CatalogRequestDto.class);
    }

    @Test
    void transform() {
        var querySpecDto = QuerySpecDto.Builder.newInstance().build();
        var querySpecJson = Json.createObjectBuilder().build();
        when(context.transform(any(), eq(QuerySpecDto.class))).thenReturn(querySpecDto);
        var json = Json.createObjectBuilder()
                .add(TYPE, EDC_CATALOG_REQUEST_TYPE)
                .add(EDC_CATALOG_REQUEST_PROTOCOL, "protocol")
                .add(EDC_CATALOG_REQUEST_PROVIDER_URL, "http://provider/url")
                .add(EDC_CATALOG_REQUEST_QUERY_SPEC, querySpecJson)
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getProtocol()).isEqualTo("protocol");
        assertThat(result.getProviderUrl()).isEqualTo("http://provider/url");
        assertThat(result.getQuerySpec()).isEqualTo(querySpecDto);
        verify(context).transform(querySpecJson, QuerySpecDto.class);
    }

}
