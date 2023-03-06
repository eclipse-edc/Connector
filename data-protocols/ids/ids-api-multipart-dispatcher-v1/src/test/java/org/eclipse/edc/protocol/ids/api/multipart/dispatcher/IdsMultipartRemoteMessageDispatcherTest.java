/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.ids.api.multipart.dispatcher;

import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.IdsMultipartSender;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class IdsMultipartRemoteMessageDispatcherTest {

    private final IdsMultipartSender sender = mock(IdsMultipartSender.class);
    private final IdsMultipartRemoteMessageDispatcher dispatcher = new IdsMultipartRemoteMessageDispatcher(sender);

    @Test
    void shouldNotSendTransferStartMessage() {
        var message = TransferStartMessage.Builder.newInstance()
                .protocol("ids-multipart")
                .connectorAddress("http://an/address")
                .build();

        var future = dispatcher.send(Object.class, message, () -> "processId");

        assertThat(future).succeedsWithin(5, SECONDS);
        verifyNoInteractions(sender);
    }

    @Test
    void shouldNotSendTransferCompletionMessage() {
        var message = TransferCompletionMessage.Builder.newInstance()
                .protocol("ids-multipart")
                .connectorAddress("http://an/address")
                .build();

        var future = dispatcher.send(Object.class, message, () -> "processId");

        assertThat(future).succeedsWithin(5, SECONDS);
        verifyNoInteractions(sender);
    }

    @Test
    void shouldNotSendTransferTerminationMessage() {
        var message = TransferTerminationMessage.Builder.newInstance()
                .protocol("ids-multipart")
                .connectorAddress("http://an/address")
                .build();

        var future = dispatcher.send(Object.class, message, () -> "processId");

        assertThat(future).succeedsWithin(5, SECONDS);
        verifyNoInteractions(sender);
    }


}
