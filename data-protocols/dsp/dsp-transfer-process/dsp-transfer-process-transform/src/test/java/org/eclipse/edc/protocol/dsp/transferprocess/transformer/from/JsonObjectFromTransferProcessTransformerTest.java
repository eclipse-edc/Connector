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
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferProcessTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CORRELATIONID_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_STATE_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFERPROCESS_TYPE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


class JsonObjectFromTransferProcessTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectFromTransferProcessTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromTransferProcessTransformer(jsonFactory);
    }

    @Test
    void transformTransferProcess() {
        var dataAddress = DataAddress.Builder.newInstance()
                .keyName("dataAddressId")
                .property("type", "TestValueProperty")
                .build();

        var dataRequest = DataRequest.Builder.newInstance()
                .id("dataRequestID")
                .dataDestination(dataAddress)
                .build();

        var transferProcess = TransferProcess.Builder.newInstance()
                .id("transferProcessID")
                .callbackAddresses(new ArrayList<>())
                .dataRequest(dataRequest)
                .type(TransferProcess.Type.PROVIDER)
                .contentDataAddress(dataAddress)
                .build();

        var result = transformer.transform(transferProcess, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_TRANSFERPROCESS_TYPE);
        assertThat(result.getJsonString(JsonLdKeywords.ID).getString()).isEqualTo("transferProcessID");
        assertThat(result.getJsonString(DSPACE_STATE_TYPE).getString()).isEqualTo("INITIAL");
        assertThat(result.getJsonString(DSPACE_CORRELATIONID_TYPE).getString()).isEqualTo("dataRequestID");

        verify(context, never()).reportProblem(anyString());
    }
}
