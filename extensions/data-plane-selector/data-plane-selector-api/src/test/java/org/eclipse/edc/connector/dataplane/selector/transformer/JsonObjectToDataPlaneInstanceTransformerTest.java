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

package org.eclipse.edc.connector.dataplane.selector.transformer;

import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_DEST_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_SOURCE_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.LAST_ACTIVE;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.TURNCOUNT;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.URL;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToDataPlaneInstanceTransformerTest {

    private final JsonObjectToDataPlaneInstanceTransformer transformer = new JsonObjectToDataPlaneInstanceTransformer();
    private final TransformerContext context = mock();

    @Test
    void transform() {
        var json = createObjectBuilder()
                .add(ID, "test-id")
                .add(URL, "http://somewhere.com:1234/api/v1")
                .add(ALLOWED_SOURCE_TYPES, createArrayBuilder(Set.of("source1", "source2")))
                .add(LAST_ACTIVE, 234)
                .add(TURNCOUNT, 42)
                .add(ALLOWED_DEST_TYPES, createArrayBuilder(Set.of("dest1", "dest2")))
                .build();

        var dpi = transformer.transform(json, context);
        assertThat(dpi).isNotNull();
        assertThat(dpi.getUrl().toString()).isEqualTo("http://somewhere.com:1234/api/v1");
        assertThat(dpi.getTurnCount()).isEqualTo(42);
        assertThat(dpi.getLastActive()).isEqualTo(234);
        assertThat(dpi.getAllowedDestTypes()).hasSize(2).containsExactlyInAnyOrder("dest1", "dest2");
        assertThat(dpi.getAllowedSourceTypes()).hasSize(2).containsExactlyInAnyOrder("source1", "source2");
    }

    @Test
    void transform_malformedUrl() {
        var json = createObjectBuilder()
                .add(ID, "test-id")
                .add(URL, "very_invalid_not_an_url")
                .add(ALLOWED_SOURCE_TYPES, createArrayBuilder(Set.of("source1", "source2")))
                .add(ALLOWED_DEST_TYPES, createArrayBuilder(Set.of("dest1", "dest2")))
                .build();

        // NPE is thrown by the builder method inside the transformer
        assertThatThrownBy(() -> transformer.transform(json, context)).isInstanceOf(NullPointerException.class);

        verify(context).reportProblem(anyString());
    }

    @BeforeEach
    void setUp() {
        when(context.transform(isA(JsonValue.class), eq(Object.class))).thenAnswer(a -> ((JsonString) a.getArgument(0)).getString());
    }
}