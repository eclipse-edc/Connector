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

package org.eclipse.edc.protocol.dsp.transferprocess.transformer.to;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferStartMessageTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.to.TestInput.getExpanded;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_DATA_ADDRESS;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToTransferStartMessageTransformerTest {

    private final String processId = "TestProcessId";

    private TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToTransferStartMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToTransferStartMessageTransformer();
    }

    @Test
    void jsonObjectToTransferStartMessage() {
        var json = Json.createObjectBuilder()
                .add(TYPE, DSPACE_TYPE_TRANSFER_START_MESSAGE)
                .add(DSPACE_PROPERTY_PROCESS_ID, processId)
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();

        assertThat(result.getProcessId()).isEqualTo(processId);

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void jsonObjectToTransferStartMessageWithDataAddress() {
        var dataAddressObject = Json.createObjectBuilder().add(EDC_NAMESPACE + "type", "AWS").build();
        var json = Json.createObjectBuilder()
                .add(TYPE, DSPACE_TYPE_TRANSFER_START_MESSAGE)
                .add(DSPACE_PROPERTY_PROCESS_ID, processId)
                .add(DSPACE_PROPERTY_DATA_ADDRESS, dataAddressObject)
                .build();

        var dataAddress = DataAddress.Builder.newInstance().type("AWS").build();

        when(context.transform(isA(JsonObject.class), eq(DataAddress.class))).thenReturn(dataAddress);

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();

        assertThat(result.getProcessId()).isEqualTo(processId);
        assertThat(result.getDataAddress()).isSameAs(dataAddress);

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void jsonObjectToTransferStartMessageWithEmptyDataAddress() {
        var json = Json.createObjectBuilder()
                .add(TYPE, DSPACE_TYPE_TRANSFER_START_MESSAGE)
                .add(DSPACE_PROPERTY_PROCESS_ID, processId)
                .add(DSPACE_PROPERTY_DATA_ADDRESS, Json.createObjectBuilder().build())
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();

        assertThat(result.getProcessId()).isEqualTo(processId);
        assertThat(result.getDataAddress()).isNull();

        verify(context, never()).reportProblem(anyString());
    }
}
