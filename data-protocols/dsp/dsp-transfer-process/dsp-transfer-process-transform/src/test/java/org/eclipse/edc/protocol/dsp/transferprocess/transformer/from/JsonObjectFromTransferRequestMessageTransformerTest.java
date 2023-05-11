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
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferRequestMessageTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CALLBACK_ADDRESS_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CONTRACT_AGREEMENT_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_DATA_ADDRESS_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PROCESSID_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFERPROCESS_REQUEST_TYPE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class JsonObjectFromTransferRequestMessageTransformerTest {

    private final String dataAddressKey = "testDataAddressKey";

    private final String dataAddressType = "testDataAddressType";

    private final String protocol = "testProtocol";

    private final String contractId = "testContractId";

    private final String callbackAddress = "http://testcallback";

    private final String id = "testId";

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
    void transformTransferRequestMessageWithDataAddress() {
        var properties = new HashMap<String, String>();
        properties.put("key", "value");

        var message = TransferRequestMessage.Builder.newInstance()
                .id(id)
                .properties(properties)
                .callbackAddress(callbackAddress)
                .contractId(contractId)
                .protocol(protocol)
                .dataDestination(buildTestDataAddress())
                .build();

        var result = transformer.transform(message, context);

        Assertions.assertNotNull(result);
        assertThat(result.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_TRANSFERPROCESS_REQUEST_TYPE);
        assertThat(result.getJsonString(DSPACE_CONTRACT_AGREEMENT_TYPE).getString()).isEqualTo(contractId);
        assertThat(result.getJsonString(DCT_FORMAT_ATTRIBUTE).getString()).isEqualTo(dataAddressType);
        assertThat(result.getJsonString(DSPACE_CALLBACK_ADDRESS_TYPE).getString()).isEqualTo(callbackAddress);
        assertThat(result.getJsonString(DSPACE_PROCESSID_TYPE).getString()).isEqualTo(id);
        assertThat(result.getJsonObject(DSPACE_DATA_ADDRESS_TYPE).getString("keyName")).isEqualTo(dataAddressKey);

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transformTransferRequestMessageWithoutDataAddress() {
        var properties = new HashMap<String, String>();
        properties.put("key", "value");

        var message = TransferRequestMessage.Builder.newInstance()
                .id(id)
                .properties(properties)
                .callbackAddress(callbackAddress)
                .contractId(contractId)
                .protocol(protocol)
                .dataDestination(DataAddress.Builder.newInstance().type(dataAddressType).build())
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_TRANSFERPROCESS_REQUEST_TYPE);
        assertThat(result.getJsonString(DSPACE_CONTRACT_AGREEMENT_TYPE).getString()).isEqualTo(contractId);
        assertThat(result.getJsonString(DCT_FORMAT_ATTRIBUTE).getString()).isEqualTo(dataAddressType);
        assertThat(result.getJsonString(DSPACE_CALLBACK_ADDRESS_TYPE).getString()).isEqualTo(callbackAddress);
        assertThat(result.getJsonString(DSPACE_PROCESSID_TYPE).getString()).isEqualTo(id);
        assertThat(result.getJsonObject(DSPACE_DATA_ADDRESS_TYPE)).isEqualTo(null);

        verify(context, never()).reportProblem(anyString());
    }

    private DataAddress buildTestDataAddress() {
        return DataAddress.Builder.newInstance()
                .keyName(dataAddressKey)
                .type(dataAddressType)
                .build();
    }
}
