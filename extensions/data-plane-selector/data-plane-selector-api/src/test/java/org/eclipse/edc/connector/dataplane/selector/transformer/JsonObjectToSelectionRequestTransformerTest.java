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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.selector.api.v2.model.SelectionRequest.DEST_ADDRESS;
import static org.eclipse.edc.connector.dataplane.selector.api.v2.model.SelectionRequest.SOURCE_ADDRESS;
import static org.eclipse.edc.connector.dataplane.selector.api.v2.model.SelectionRequest.STRATEGY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToSelectionRequestTransformerTest {

    private final JsonObjectToSelectionRequestTransformer transformer = new JsonObjectToSelectionRequestTransformer();
    private final TransformerContext context = mock();


    @BeforeEach
    void setUp() {
        when(context.transform(isA(JsonObject.class), eq(DataAddress.class))).thenReturn(DataAddress.Builder.newInstance().type("test-type").build());
    }

    @ParameterizedTest
    @ValueSource(strings = "test-strategy")
    @NullSource
    void transform(String strategy) {
        var builder = Json.createObjectBuilder()
                .add(SOURCE_ADDRESS, createObjectBuilder()
                        .add(TYPE, DataAddress.EDC_DATA_ADDRESS_TYPE)
                        .add(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY, "test-type")
                        .add(DataAddress.EDC_DATA_ADDRESS_KEY_NAME, "test-key")
                )
                .add(DEST_ADDRESS, createObjectBuilder()
                        .add(TYPE, DataAddress.EDC_DATA_ADDRESS_TYPE)
                        .add(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY, "test-type")
                        .add(DataAddress.EDC_DATA_ADDRESS_KEY_NAME, "test-key")
                );
        if (strategy != null) {
            builder.add(STRATEGY, strategy);
        }
        var jsonObject = builder.build();

        var rq = transformer.transform(jsonObject, context);
        assertThat(rq).isNotNull();
        assertThat(rq.getStrategy()).isEqualTo(strategy);
        assertThat(rq.getDestination()).isNotNull();
        assertThat(rq.getSource()).isNotNull();
    }

}