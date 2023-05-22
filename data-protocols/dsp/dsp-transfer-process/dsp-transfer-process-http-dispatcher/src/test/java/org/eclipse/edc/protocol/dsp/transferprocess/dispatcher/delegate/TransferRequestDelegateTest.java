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

package org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.delegate;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.testfixtures.dispatcher.DspHttpDispatcherDelegateTestBase;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.TransferProcessApiPaths.TRANSFER_INITIAL_REQUEST;

class TransferRequestDelegateTest extends DspHttpDispatcherDelegateTestBase<TransferRequestMessage> {

    private TransferRequestDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new TransferRequestDelegate(serializer);
    }

    @Test
    void getMessageType() {
        assertThat(delegate.getMessageType()).isEqualTo(TransferRequestMessage.class);
    }

    @Test
    void buildRequest() throws IOException {
        var message = message();
        testBuildRequest_shouldReturnRequest(message, BASE_PATH + TRANSFER_INITIAL_REQUEST);
    }

    @Test
    void buildRequest_serializationFails_throwException() {
        testBuildRequest_shouldThrowException_whenSerializationFails(message());
    }

    @Test
    void parseResponse_returnNull() {
        testParseResponse_shouldReturnNullFunction_whenResponseBodyNotProcessed();
    }

    @Override
    protected DspHttpDispatcherDelegate<TransferRequestMessage, ?> delegate() {
        return delegate;
    }

    private TransferRequestMessage message() {
        return TransferRequestMessage.Builder.newInstance()
                .processId("testId")
                .protocol("dataspace-protocol")
                .callbackAddress("http://test-connector-address")
                .counterPartyAddress("http://test-connector-address")
                .contractId("contractId")
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("type")
                        .build())
                .build();
    }

    private JsonObject getJsonObject() {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder().add("prefix", "http://schema").build())
                .add("prefix:key", "value")
                .build();
    }
}
