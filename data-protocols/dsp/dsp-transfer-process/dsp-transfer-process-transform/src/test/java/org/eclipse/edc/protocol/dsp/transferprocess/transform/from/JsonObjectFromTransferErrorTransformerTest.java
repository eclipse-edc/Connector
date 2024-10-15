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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferError;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from.JsonObjectFromTransferErrorTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_ERROR;
import static org.mockito.Mockito.mock;

class JsonObjectFromTransferErrorTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectFromTransferErrorTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromTransferErrorTransformer(jsonFactory);
    }

    @Test
    void transform_returnJsonObject() {

        var error = TransferError.Builder.newInstance()
                .code("code")
                .messages(List.of("message"))
                .build();

        var result = transformer.transform(error, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DSPACE_TYPE_TRANSFER_ERROR);
        assertThat(result.getString(DSPACE_PROPERTY_CODE_IRI)).isEqualTo("code");
        assertThat(result.getJsonArray(DSPACE_PROPERTY_REASON_IRI)).contains(Json.createValue("message"));
    }
}
