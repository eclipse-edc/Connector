package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Initiate;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.PrepareProvision;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitiateHandlerTest {

    private final TransferProcessStore transferProcessStore = mock(TransferProcessStore.class);

    @Test
    void should_create_transfer_process_if_it_does_not_exist() {
        var handler = new InitiateHandler(transferProcessStore);
        var dataRequest = DataRequest.Builder.newInstance()
                .id("dataRequestId")
                .dataDestination(DataAddress.Builder.newInstance().build())
                .build();
        when(transferProcessStore.processIdForTransferId("dataRequestId")).thenReturn(null);

        var result = handler.handle(new Initiate("transferProcessId", TransferProcess.Type.CONSUMER, dataRequest));

        assertThat(result.getNextCommand()).isInstanceOf(PrepareProvision.class);
        verify(transferProcessStore).create(isA(TransferProcess.class));
    }

    @Test
    void should_not_create_transfer_process_if_it_already_exist() {
        var handler = new InitiateHandler(transferProcessStore);
        var dataRequest = DataRequest.Builder.newInstance()
                .id("dataRequestId")
                .dataDestination(DataAddress.Builder.newInstance().build())
                .build();
        when(transferProcessStore.processIdForTransferId("dataRequestId")).thenReturn("transferProcessId");

        var result = handler.handle(new Initiate("transferProcessId", TransferProcess.Type.CONSUMER, dataRequest));

        assertThat(result.getNextCommand()).isInstanceOf(PrepareProvision.class);
        verify(transferProcessStore, never()).create(any());
    }


}