/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.dsp.catalog.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogError;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_ERROR_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_IRI;
import static org.mockito.Mockito.mock;

class JsonObjectFromCatalogErrorTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectFromCatalogErrorTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromCatalogErrorTransformer(jsonFactory);
    }

    @Test
    void transform_returnJsonObject() {

        var error = CatalogError.Builder.newInstance()
                .code("code")
                .messages(List.of("message"))
                .build();

        var result = transformer.transform(error, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DSPACE_TYPE_CATALOG_ERROR_IRI);
        assertThat(result.getString(DSPACE_PROPERTY_CODE_IRI)).isEqualTo("code");
        assertThat(result.getJsonArray(DSPACE_PROPERTY_REASON_IRI)).contains(Json.createValue("message"));
    }
}
