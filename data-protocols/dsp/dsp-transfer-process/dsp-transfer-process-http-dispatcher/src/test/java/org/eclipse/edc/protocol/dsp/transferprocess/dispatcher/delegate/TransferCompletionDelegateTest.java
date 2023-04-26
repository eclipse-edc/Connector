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

import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.testfixtures.dispatcher.DspHttpDispatcherDelegateTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.TransferProcessApiPaths.TRANSFER_COMPLETION;

class TransferCompletionDelegateTest extends DspHttpDispatcherDelegateTestBase<TransferCompletionMessage> {

    private TransferCompletionDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new TransferCompletionDelegate(serializer);
    }

    @Test
    void getMessageType() {
        assertThat(delegate.getMessageType()).isEqualTo(TransferCompletionMessage.class);
    }

    @Test
    void buildRequest() throws IOException {
        var message = message();
        testBuildRequest_shouldReturnRequest(message, BASE_PATH + message.getProcessId() + TRANSFER_COMPLETION);
    }

    @Test
    void buildRequest_serializationFails_throwException() {
        testBuildRequest_shouldThrowException_whenSerializationFails(message());
    }

    @Test
    void parseResponse_returnNull() {
        testParseResponse_shouldReturnNullFunction_whenResponseBodyNotProcessed();
    }

    private TransferCompletionMessage message() {
        return TransferCompletionMessage.Builder.newInstance()
                .processId("testId")
                .protocol("dataspace-protocol")
                .callbackAddress("http://test-connector-address")
                .build();
    }

    @Override
    protected DspHttpDispatcherDelegate<TransferCompletionMessage, ?> delegate() {
        return delegate;
    }
}
