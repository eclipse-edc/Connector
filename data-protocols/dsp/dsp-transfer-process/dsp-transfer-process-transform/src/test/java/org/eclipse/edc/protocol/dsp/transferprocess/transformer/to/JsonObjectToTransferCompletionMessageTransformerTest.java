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
import org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferCompletionMessageTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PROCESSID_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFER_COMPLETION_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.to.TestInput.getExpanded;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectToTransferCompletionMessageTransformerTest {

    private final String processId = "TestProcessId";

    private TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToTransferCompletionMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToTransferCompletionMessageTransformer();
    }

    @Test
    void jsonObjectToTransferCompletionMessage() {

        var json = Json.createObjectBuilder()
                .add(TYPE, DSPACE_TRANSFER_COMPLETION_TYPE)
                .add(DSPACE_PROCESSID_TYPE, processId)
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();

        assertThat(result.getProcessId()).isEqualTo(processId);
        assertThat(result.getProtocol()).isEqualTo(HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP);

        verify(context, never()).reportProblem(anyString());
    }
}
