package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResourceSet;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.eclipse.dataspaceconnector.transfer.core.transfer.TestProvisionedResource;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Complete;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.InitiateDataFlowConsumer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.IN_PROGRESS;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTED_ACK;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.STREAMING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitiateDataFlowConsumerHandlerTest {

    private final TransferProcessStore store = mock(TransferProcessStore.class);

    @Test
    void should_progress_if_finite_and_managed_resource_and_provisioned() {
        var dataRequest = DataRequest.Builder.newInstance()
                .managedResources(true)
                .transferType(TransferType.Builder.transferType().isFinite(true).build())
                .dataDestination(DataAddress.Builder.newInstance().build())
                .build();
        var transferProcess = TransferProcess.Builder.newInstance()
                .id("tpId")
                .dataRequest(dataRequest)
                .state(REQUESTED_ACK.code())
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().resources(List.of(new TestProvisionedResource())).build())
                .build();
        when(store.find("tpId")).thenReturn(transferProcess);
        var handler = new InitiateDataFlowConsumerHandler(store, mock(Monitor.class));

        var result = handler.handle(new InitiateDataFlowConsumer("tpId"));

        assertThat(result.getNextCommand()).isInstanceOf(Complete.class);
        verify(store).update(Mockito.argThat(process -> process.getState() == IN_PROGRESS.code()));
    }

    @Test
    void should_streaming_if_non_finite_and_managed_resource_and_provisioned() {
        var dataRequest = DataRequest.Builder.newInstance()
                .managedResources(true)
                .transferType(TransferType.Builder.transferType().isFinite(false).build())
                .dataDestination(DataAddress.Builder.newInstance().build())
                .build();
        var transferProcess = TransferProcess.Builder.newInstance()
                .id("tpId")
                .dataRequest(dataRequest)
                .state(REQUESTED_ACK.code())
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().resources(List.of(new TestProvisionedResource())).build())
                .build();
        when(store.find("tpId")).thenReturn(transferProcess);
        var handler = new InitiateDataFlowConsumerHandler(store, mock(Monitor.class));

        var result = handler.handle(new InitiateDataFlowConsumer("tpId"));

        assertThat(result.getNextCommand()).isNull();
        verify(store).update(Mockito.argThat(process -> process.getState() == STREAMING.code()));
    }

    @Test
    void should_not_do_anything_if_provisioned_resources_are_empty() {
        var dataRequest = DataRequest.Builder.newInstance()
                .managedResources(true)
                .transferType(TransferType.Builder.transferType().isFinite(false).build())
                .dataDestination(DataAddress.Builder.newInstance().build())
                .build();
        var transferProcess = TransferProcess.Builder.newInstance()
                .id("tpId")
                .dataRequest(dataRequest)
                .state(REQUESTED_ACK.code())
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
                .build();
        when(store.find("tpId")).thenReturn(transferProcess);
        var handler = new InitiateDataFlowConsumerHandler(store, mock(Monitor.class));

        var result = handler.handle(new InitiateDataFlowConsumer("tpId"));

        assertThat(result.getNextCommand()).isNull();
        verify(store, never()).update(any());
    }

}