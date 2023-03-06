package org.eclipse.edc.protocol.ids.api.multipart.dispatcher;

import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.IdsMultipartSender;
import org.eclipse.edc.spi.message.MessageContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class IdsMultipartRemoteMessageDispatcherTest {

    @Test
    void shouldNotSendTransferStartMessage() {
        var sender = mock(IdsMultipartSender.class);
        var dispatcher = new IdsMultipartRemoteMessageDispatcher(sender);

        var message = TransferStartMessage.Builder.newInstance()
                .protocol("ids-multipart")
                .connectorAddress("http://an/address")
                .build();

        var future = dispatcher.send(Object.class, message, () -> "processId");

        assertThat(future).succeedsWithin(5, SECONDS);
        verifyNoInteractions(sender);
    }
}
