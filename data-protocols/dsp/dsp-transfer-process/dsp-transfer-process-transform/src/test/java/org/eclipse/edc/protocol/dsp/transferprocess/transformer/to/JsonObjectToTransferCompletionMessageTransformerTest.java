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
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferCompletionMessageTransformer;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.to.TestInput.getExpanded;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToTransferCompletionMessageTransformerTest {

    private final TransformerContext context = mock();

    private final JsonObjectToTransferCompletionMessageTransformer transformer =
            new JsonObjectToTransferCompletionMessageTransformer();

    @Test
    void shouldTransform() {
        var json = Json.createObjectBuilder()
                .add(TYPE, DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE)
                .add(DSPACE_PROPERTY_PROVIDER_PID, "providerPid")
                .add(DSPACE_PROPERTY_CONSUMER_PID, "consumerPid")
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();
        assertThat(result.getProviderPid()).isEqualTo("providerPid");
        assertThat(result.getConsumerPid()).isEqualTo("consumerPid");

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void shouldReportError_whenConsumerPidAndProviderPidNotSet() {
        when(context.problem()).thenReturn(new ProblemBuilder(context));
        var json = Json.createObjectBuilder()
                .add(TYPE, DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE)
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNull();

        verify(context).reportProblem(anyString());
    }

    @Deprecated(since = "0.4.1")
    @Test
    void shouldSetProcessIdAsConsumerAndProviderPid_whenProcessIdIsSet() {
        var json = Json.createObjectBuilder()
                .add(TYPE, DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE)
                .add(DSPACE_PROPERTY_PROCESS_ID, "TestProcessId")
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();
        assertThat(result.getConsumerPid()).isEqualTo("TestProcessId");
        assertThat(result.getProviderPid()).isEqualTo("TestProcessId");

        verify(context, never()).reportProblem(anyString());
    }
}
