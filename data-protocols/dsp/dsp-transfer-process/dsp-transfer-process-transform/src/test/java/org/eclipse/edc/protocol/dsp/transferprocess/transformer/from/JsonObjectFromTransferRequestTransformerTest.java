/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transformer.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.JsonLdKeywords;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferRequestMessageTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DCT_FORMAT;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_CALLBACKADDRESS_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_CONTRACTAGREEMENT_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_PROCESSID_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_TRANSFERPROCESS_REQUEST_TYPE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class JsonObjectFromTransferRequestTransformerTest {

    private final String dataAddressKey = "testDataAddressKey";

    private final String dataAddressType = "testDataAddressType";

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectFromTransferRequestMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        var dataAddressJson = Json.createObjectBuilder()
                .add("keyName", dataAddressKey)
                .add("type", dataAddressType)
                .build();

        transformer = new JsonObjectFromTransferRequestMessageTransformer(jsonFactory);

        when(context.transform(isA(DataAddress.class), eq(JsonObject.class))).thenReturn(dataAddressJson);
    }



    @Test
    void transformTransferRequestMessage() {
        var properties = new HashMap<String, String>();
        properties.put("key", "value");

        var message = TransferRequestMessage.Builder.newInstance()
                .id("TestID")
                .properties(properties)
                .callbackAddress("TestConnectorAddress")
                .contractId("ContractID")
                .protocol("dsp")
                .dataDestination(buildTestDataAddress())
                .build();

        var result = transformer.transform(message, context);

        Assertions.assertNotNull(result);
        assertThat(result.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_TRANSFERPROCESS_REQUEST_TYPE);
        assertThat(result.getJsonString(DSPACE_CONTRACTAGREEMENT_TYPE).getString()).isEqualTo("ContractID");
        assertThat(result.getJsonString(DCT_FORMAT).getString()).isEqualTo(dataAddressType);
        assertThat(result.getJsonString(DSPACE_CALLBACKADDRESS_TYPE).getString()).isEqualTo("TestConnectorAddress");
        assertThat(result.getJsonString(DSPACE_PROCESSID_TYPE).getString()).isEqualTo("TestID");
        //TODO Add missing fields (dataAddress) from Spec

        verify(context, never()).reportProblem(anyString());
    }

    private DataAddress buildTestDataAddress() {
        return DataAddress.Builder.newInstance()
                .keyName(dataAddressKey)
                .type(dataAddressType)
                .build();
    }
}
