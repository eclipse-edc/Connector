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

package org.eclipse.edc.transform.transformer.edc.to;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_SOURCE_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_TRANSFER_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.LAST_ACTIVE;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.URL;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToDataPlaneInstanceTransformerTest {

    private final JsonObjectToDataPlaneInstanceTransformer transformer = new JsonObjectToDataPlaneInstanceTransformer();
    private final TransformerContext context = mock();
    private final TitaniumJsonLd jsonLd = new TitaniumJsonLd(mock());

    @BeforeEach
    void setUp() {
        when(context.transform(isA(JsonValue.class), eq(Object.class))).thenAnswer(a -> ((JsonObject) a.getArgument(0)).getString(VALUE));
        when(context.transform(isA(JsonValue.class), eq(String.class))).thenAnswer(a -> ((JsonObject) a.getArgument(0)).getString(VALUE));
    }

    @Test
    void transform() {
        var json = createObjectBuilder()
                .add(ID, "test-id")
                .add(URL, "http://somewhere.com:1234/api/v1")
                .add(ALLOWED_SOURCE_TYPES, createArrayBuilder(Set.of("source1", "source2")))
                .add(LAST_ACTIVE, 234L)
                .build();

        var dpi = transformer.transform(expand(json), context);
        assertThat(dpi).isNotNull();
        assertThat(dpi.getUrl().toString()).isEqualTo("http://somewhere.com:1234/api/v1");
        assertThat(dpi.getLastActive()).isEqualTo(234L);
        assertThat(dpi.getAllowedSourceTypes()).hasSize(2).containsExactlyInAnyOrder("source1", "source2");
    }

    @Test
    void transform_withProperties() {
        var jsonObject = Json.createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(ID, UUID.randomUUID().toString())
                .add(EDC_NAMESPACE + "url", "http://localhost/control/transfer")
                .add(EDC_NAMESPACE + "allowedSourceTypes", createArrayBuilder(List.of("HttpData", "HttpProvision", "Kafka")))
                .add(EDC_NAMESPACE + "allowedDestTypes", createArrayBuilder(List.of("HttpData", "HttpProvision", "HttpProxy", "Kafka")))
                .add(EDC_NAMESPACE + "properties", createObjectBuilder().add("publicApiUrl", "http://localhost/public"))
                .build();

        var dpi = transformer.transform(expand(jsonObject), context);
        assertThat(dpi).isNotNull();
        assertThat(dpi.getProperties()).containsEntry(EDC_NAMESPACE + "publicApiUrl", "http://localhost/public");
    }

    @Test
    void transform_withTransferTypes() {
        var json = createObjectBuilder()
                .add(ID, "test-id")
                .add(URL, "http://somewhere.com:1234/api/v1")
                .add(ALLOWED_SOURCE_TYPES, createArrayBuilder(Set.of("source1", "source2")))
                .add(LAST_ACTIVE, 234L)
                .add(ALLOWED_TRANSFER_TYPES, createArrayBuilder(Set.of("transfer1", "transfer2")))
                .build();

        var dpi = transformer.transform(expand(json), context);
        assertThat(dpi).isNotNull();
        assertThat(dpi.getUrl().toString()).isEqualTo("http://somewhere.com:1234/api/v1");
        assertThat(dpi.getLastActive()).isEqualTo(234L);
        assertThat(dpi.getAllowedSourceTypes()).hasSize(2).containsExactlyInAnyOrder("source1", "source2");
        assertThat(dpi.getAllowedTransferTypes()).hasSize(2).containsExactlyInAnyOrder("transfer1", "transfer2");
    }

    @Test
    void transform_malformedUrl() {
        var json = createObjectBuilder()
                .add(ID, "test-id")
                .add(URL, "very_invalid_not_an_url")
                .add(ALLOWED_SOURCE_TYPES, createArrayBuilder(Set.of("source1", "source2")))
                .build();

        // NPE is thrown by the builder method inside the transformer
        assertThatThrownBy(() -> transformer.transform(expand(json), context)).isInstanceOf(NullPointerException.class);

        verify(context).reportProblem(anyString());
    }

    private JsonObject expand(JsonObject jsonObject) {
        return jsonLd.expand(jsonObject).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
    }
}
