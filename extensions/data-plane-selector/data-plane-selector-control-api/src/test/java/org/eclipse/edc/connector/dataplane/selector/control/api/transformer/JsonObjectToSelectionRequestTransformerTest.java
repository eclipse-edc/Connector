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

package org.eclipse.edc.connector.dataplane.selector.control.api.transformer;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.selector.control.api.model.SelectionRequest.SOURCE_ADDRESS;
import static org.eclipse.edc.connector.dataplane.selector.control.api.model.SelectionRequest.STRATEGY;
import static org.eclipse.edc.connector.dataplane.selector.control.api.model.SelectionRequest.TRANSFER_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToSelectionRequestTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectToSelectionRequestTransformer transformer = new JsonObjectToSelectionRequestTransformer();

    @Test
    void transform() {
        when(context.transform(isA(JsonObject.class), eq(DataAddress.class))).thenReturn(DataAddress.Builder.newInstance().type("test-type").build());

        var jsonObject = Json.createObjectBuilder()
                .add(SOURCE_ADDRESS, createObjectBuilder()
                        .add(TYPE, DataAddress.EDC_DATA_ADDRESS_TYPE)
                        .add(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY, "test-type")
                        .add(DataAddress.EDC_DATA_ADDRESS_KEY_NAME, "test-key")
                )
                .add(TRANSFER_TYPE, "transfer-type")
                .add(STRATEGY, "strategy")
                .build();

        var result = transformer.transform(jsonObject, context);

        assertThat(result).isNotNull();
        assertThat(result.getStrategy()).isEqualTo("strategy");
        assertThat(result.getSource()).isNotNull();
        assertThat(result.getTransferType()).isEqualTo("transfer-type");
    }

}
