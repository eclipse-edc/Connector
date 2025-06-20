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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.to;

import jakarta.json.Json;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferCompletionMessageTransformer;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.transferprocess.transform.to.TestInput.getExpanded;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToTransferCompletionMessageTransformerTest {

    private static final JsonLdNamespace DSP_NAMESPACE = new JsonLdNamespace("http://www.w3.org/ns/dsp#");
    private final TransformerContext context = mock();

    private final JsonObjectToTransferCompletionMessageTransformer transformer =
            new JsonObjectToTransferCompletionMessageTransformer(DSP_NAMESPACE);

    @Test
    void shouldTransform() {
        var json = Json.createObjectBuilder()
                .add(TYPE, DSP_NAMESPACE.toIri(DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM))
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM), "providerPid")
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM), "consumerPid")
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
                .add(TYPE, DSP_NAMESPACE.toIri(DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM))
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNull();

        verify(context).reportProblem(anyString());
    }

}
