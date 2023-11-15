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
import org.eclipse.edc.catalog.spi.CatalogRequest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROTOCOL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROVIDER_URL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_QUERY_SPEC;
import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JsonObjectToCatalogRequestTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectToCatalogRequestTransformer transformer = new JsonObjectToCatalogRequestTransformer();

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getOutputType()).isEqualTo(CatalogRequest.class);
    }

    @Test
    void transform() {
        var querySpec = QuerySpec.Builder.newInstance().build();
        var querySpecJson = Json.createObjectBuilder().build();
        when(context.transform(any(), eq(QuerySpec.class))).thenReturn(querySpec);
        var json = Json.createObjectBuilder()
                .add(TYPE, CATALOG_REQUEST_TYPE)
                .add(CATALOG_REQUEST_PROTOCOL, "protocol")
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, "http://provider/url")
                .add(CATALOG_REQUEST_QUERY_SPEC, querySpecJson)
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getProtocol()).isEqualTo("protocol");
        assertThat(result.getCounterPartyAddress()).isEqualTo("http://provider/url");
        assertThat(result.getQuerySpec()).isEqualTo(querySpec);
        verify(context).transform(querySpecJson, QuerySpec.class);
    }

    @Test
    void transform_shouldUseProviderId_whenCounterPartyAddressIsMissing() {
        var querySpec = QuerySpec.Builder.newInstance().build();
        var querySpecJson = Json.createObjectBuilder().build();
        when(context.transform(any(), eq(QuerySpec.class))).thenReturn(querySpec);
        var json = Json.createObjectBuilder()
                .add(TYPE, CATALOG_REQUEST_TYPE)
                .add(CATALOG_REQUEST_PROTOCOL, "protocol")
                .add(CATALOG_REQUEST_PROVIDER_URL, "http://provider/url")
                .add(CATALOG_REQUEST_QUERY_SPEC, querySpecJson)
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getProtocol()).isEqualTo("protocol");
        assertThat(result.getCounterPartyAddress()).isEqualTo("http://provider/url");
        assertThat(result.getQuerySpec()).isEqualTo(querySpec);
        verify(context).transform(querySpecJson, QuerySpec.class);
    }

    @Test
    void transform_shouldHandleEmptyQuerySpec() {
        var json = Json.createObjectBuilder()
                .add(TYPE, CATALOG_REQUEST_TYPE)
                .add(CATALOG_REQUEST_PROTOCOL, "protocol")
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, "http://provider/url")
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getQuerySpec()).isEqualTo(null);
        verifyNoInteractions(context);
    }

}
