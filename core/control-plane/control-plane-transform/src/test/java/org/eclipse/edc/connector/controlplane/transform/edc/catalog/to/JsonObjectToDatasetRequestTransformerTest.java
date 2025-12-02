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

package org.eclipse.edc.connector.controlplane.transform.edc.catalog.to;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest.DATASET_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest.DATASET_REQUEST_COUNTER_PARTY_ID;
import static org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest.DATASET_REQUEST_PROTOCOL;
import static org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest.DATASET_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToDatasetRequestTransformerTest {

    private final JsonObjectToDatasetRequestTransformer transformer = new JsonObjectToDatasetRequestTransformer();
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getOutputType()).isEqualTo(DatasetRequest.class);
    }

    @Test
    void transform() {
        var querySpec = QuerySpec.Builder.newInstance().build();
        when(context.transform(any(), eq(QuerySpec.class))).thenReturn(querySpec);
        var json = Json.createObjectBuilder()
                .add(TYPE, DATASET_REQUEST_TYPE)
                .add(ID, "id")
                .add(DATASET_REQUEST_PROTOCOL, "protocol")
                .add(DATASET_REQUEST_COUNTER_PARTY_ADDRESS, "http://provider/url")
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("id");
        assertThat(result.getProtocol()).isEqualTo("protocol");
        assertThat(result.getCounterPartyAddress()).isEqualTo("http://provider/url");
        assertThat(result.getCounterPartyId()).isEqualTo("http://provider/url");
    }

    @Test
    void transform_shouldUseCounterPartyId_whenProvided() {
        var querySpec = QuerySpec.Builder.newInstance().build();
        when(context.transform(any(), eq(QuerySpec.class))).thenReturn(querySpec);
        var json = Json.createObjectBuilder()
                .add(TYPE, DATASET_REQUEST_TYPE)
                .add(ID, "id")
                .add(DATASET_REQUEST_PROTOCOL, "protocol")
                .add(DATASET_REQUEST_COUNTER_PARTY_ADDRESS, "http://provider/url")
                .add(DATASET_REQUEST_COUNTER_PARTY_ID, "providerId")
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("id");
        assertThat(result.getProtocol()).isEqualTo("protocol");
        assertThat(result.getCounterPartyAddress()).isEqualTo("http://provider/url");
        assertThat(result.getCounterPartyId()).isEqualTo("providerId");

    }

}
