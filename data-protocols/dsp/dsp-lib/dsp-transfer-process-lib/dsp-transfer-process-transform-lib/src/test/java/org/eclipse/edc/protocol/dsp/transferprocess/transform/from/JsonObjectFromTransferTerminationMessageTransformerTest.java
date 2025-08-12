/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from.JsonObjectFromTransferTerminationMessageTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


class JsonObjectFromTransferTerminationMessageTransformerTest {

    private static final JsonLdNamespace DSP_NAMESPACE = new JsonLdNamespace("http://www.w3.org/ns/dsp#");
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectFromTransferTerminationMessageTransformer transformer =
            new JsonObjectFromTransferTerminationMessageTransformer(jsonFactory, DSP_NAMESPACE);

    @Test
    void transformTransferTerminationMessage() {
        var message = TransferTerminationMessage.Builder.newInstance()
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .protocol("dsp")
                .code("testCode")
                .reason("testReason")
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSP_NAMESPACE.toIri(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM));
        assertThat(result.getJsonString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM)).getString()).isEqualTo("consumerPid");
        assertThat(result.getJsonString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM)).getString()).isEqualTo("providerPid");
        assertThat(result.getJsonString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CODE_TERM)).getString()).isEqualTo("testCode");
        assertThat(result.getJsonString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_REASON_TERM)).getString()).isEqualTo("testReason");

        verify(context, never()).reportProblem(anyString());
    }
}
