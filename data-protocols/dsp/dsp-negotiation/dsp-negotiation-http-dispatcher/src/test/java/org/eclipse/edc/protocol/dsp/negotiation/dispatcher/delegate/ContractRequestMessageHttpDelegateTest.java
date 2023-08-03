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

package org.eclipse.edc.protocol.dsp.negotiation.dispatcher.delegate;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.testfixtures.dispatcher.DspHttpDispatcherDelegateTestBase;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_REQUESTED;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_STATE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractRequestMessageHttpDelegateTest extends DspHttpDispatcherDelegateTestBase<ContractRequestMessage> {
    private static final String PROCESS_ID = "processId";

    private ContractRequestMessageHttpDelegate delegate;
    private TitaniumJsonLd jsonLdService;

    @BeforeEach
    void setUp() {
        jsonLdService = new TitaniumJsonLd(mock(Monitor.class));
        delegate = new ContractRequestMessageHttpDelegate(serializer, mapper, jsonLdService);
    }

    @Test
    void parseResponse_returnNegotiation() throws IOException {
        var response = mock(Response.class);
        var responseBody = mock(ResponseBody.class);
        var bytes = "test".getBytes();

        var expanded = jsonLdService.expand(negotiation()).getContent();

        when(response.body()).thenReturn(responseBody);
        when(responseBody.bytes()).thenReturn(bytes);
        when(mapper.readValue(bytes, JsonObject.class)).thenReturn(negotiation());

        var result = delegate.parseResponse().apply(response);

        assertThat(result).isEqualTo(expanded);
        verify(mapper, times(1)).readValue(bytes, JsonObject.class);
    }

    @Test
    void parseResponse_responseBodyNull_throwException() {
        testParseResponse_shouldThrowException_whenResponseBodyNull();
    }

    @Test
    void parseResponse_readingResponseBodyFails_throwException() throws IOException {
        testParseResponse_shouldThrowException_whenReadingResponseBodyFails();
    }

    @Override
    protected DspHttpDispatcherDelegate<ContractRequestMessage, ?> delegate() {
        return delegate;
    }

    private JsonObject negotiation() {
        var builder = Json.createObjectBuilder();
        builder.add(JsonLdKeywords.ID, "id1");
        builder.add(JsonLdKeywords.TYPE, DSPACE_TYPE_CONTRACT_NEGOTIATION);

        builder.add(DSPACE_PROPERTY_PROCESS_ID, PROCESS_ID);
        builder.add(DSPACE_PROPERTY_STATE, DSPACE_VALUE_NEGOTIATION_STATE_REQUESTED);

        return builder.build();
    }
}
