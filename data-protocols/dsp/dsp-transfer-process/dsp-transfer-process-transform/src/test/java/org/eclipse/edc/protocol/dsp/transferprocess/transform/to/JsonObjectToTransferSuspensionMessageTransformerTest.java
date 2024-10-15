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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.to;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferSuspensionMessageTransformer;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transform.to.TestInput.getExpanded;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToTransferSuspensionMessageTransformerTest {

    private final TransformerContext context = mock();
    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final JsonObjectToTransferSuspensionMessageTransformer transformer =
            new JsonObjectToTransferSuspensionMessageTransformer(objectMapper);

    @Test
    void shouldTransform() {
        var json = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(VOCAB, DSPACE_SCHEMA))
                .add(TYPE, DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE)
                .add(DSPACE_PROPERTY_CONSUMER_PID, "consumerPid")
                .add(DSPACE_PROPERTY_PROVIDER_PID, "providerPid")
                .add(DSPACE_PROPERTY_CODE_IRI, "testCode")
                .add(DSPACE_PROPERTY_REASON_IRI, Json.createArrayBuilder()
                        .add(createObjectBuilder().add("complex", "reason"))
                        .add("reason"))
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();

        assertThat(result.getProviderPid()).isEqualTo("providerPid");
        assertThat(result.getConsumerPid()).isEqualTo("consumerPid");
        assertThat(result.getReason()).hasSize(2)
                .containsOnly(Map.of(DSPACE_SCHEMA + "complex", List.of(Map.of(VALUE, "reason"))), Map.of(VALUE, "reason"));
        assertThat(result.getCode()).isEqualTo("testCode");

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void shouldReportError_whenMissingPids() {
        when(context.problem()).thenReturn(new ProblemBuilder(context));
        var reason = Json.createBuilderFactory(Map.of()).createObjectBuilder().add(DSPACE_SCHEMA + "foo", "bar");
        var reasonArray = Json.createBuilderFactory(Map.of()).createArrayBuilder().add(reason).build();

        var json = createObjectBuilder()
                .add(TYPE, DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE)
                .add(DSPACE_PROPERTY_CODE_IRI, "testCode")
                .add(DSPACE_PROPERTY_REASON_IRI, Json.createBuilderFactory(Map.of()).createArrayBuilder().add(reasonArray).build())
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNull();
        verify(context).reportProblem(anyString());
    }

}
