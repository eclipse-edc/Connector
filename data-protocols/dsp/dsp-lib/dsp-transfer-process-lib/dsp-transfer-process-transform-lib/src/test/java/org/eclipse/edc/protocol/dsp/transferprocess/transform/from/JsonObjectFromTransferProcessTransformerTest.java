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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from.JsonObjectFromTransferProcessTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_STATE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_PROCESS_TERM;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


class JsonObjectFromTransferProcessTransformerTest {

    private static final JsonLdNamespace DSP_NAMESPACE = new JsonLdNamespace("http://www.w3.org/ns/dsp#");
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectFromTransferProcessTransformer transformer =
            new JsonObjectFromTransferProcessTransformer(jsonFactory, DSP_NAMESPACE);

    @Test
    void transformTransferProcessProvider() {
        var dataAddress = DataAddress.Builder.newInstance()
                .keyName("dataAddressId")
                .property("type", "TestValueProperty")
                .build();

        var transferProcess = TransferProcess.Builder.newInstance()
                .id("providerPid")
                .callbackAddresses(new ArrayList<>())
                .correlationId("consumerPid")
                .dataDestination(dataAddress)
                .type(TransferProcess.Type.PROVIDER)
                .contentDataAddress(dataAddress)
                .build();

        var result = transformer.transform(transferProcess, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DSP_NAMESPACE.toIri(DSPACE_TYPE_TRANSFER_PROCESS_TERM));
        assertThat(result.getJsonString(ID).getString()).isEqualTo("providerPid");
        assertThat(result.getJsonString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_STATE_TERM)).getString()).isEqualTo("INITIAL");
        assertThat(result.getJsonString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM)).getString()).isEqualTo("consumerPid");
        assertThat(result.getJsonString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM)).getString()).isEqualTo("providerPid");

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transformTransferProcessConsumer() {
        var dataAddress = DataAddress.Builder.newInstance()
                .keyName("dataAddressId")
                .property("type", "TestValueProperty")
                .build();

        var transferProcess = TransferProcess.Builder.newInstance()
                .id("consumerPid")
                .callbackAddresses(new ArrayList<>())
                .correlationId("providerPid")
                .dataDestination(dataAddress)
                .type(TransferProcess.Type.CONSUMER)
                .contentDataAddress(dataAddress)
                .build();

        var result = transformer.transform(transferProcess, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DSP_NAMESPACE.toIri(DSPACE_TYPE_TRANSFER_PROCESS_TERM));
        assertThat(result.getJsonString(ID).getString()).isEqualTo("consumerPid");
        assertThat(result.getJsonString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_STATE_TERM)).getString()).isEqualTo("INITIAL");
        assertThat(result.getJsonString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM)).getString()).isEqualTo("consumerPid");
        assertThat(result.getJsonString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM)).getString()).isEqualTo("providerPid");

        verify(context, never()).reportProblem(anyString());
    }
}
