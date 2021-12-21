package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessListener;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResourceSet;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusChecker;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Complete;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.IN_PROGRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompleteHandlerTest {

    private final TransferProcessStore store = mock(TransferProcessStore.class);
    private final StatusCheckerRegistry statusCheckerRegistry = mock(StatusCheckerRegistry.class);

    private final CompleteHandler handler = new CompleteHandler(store, statusCheckerRegistry, mock(Monitor.class));

    @Test
    void should_not_transition_process_with_managed_resources_but_no_status_checker() {
        var dataRequest = DataRequest.Builder.newInstance().managedResources(true).dataDestination(DataAddress.Builder.newInstance().build()).build();
        when(store.find("tpId")).thenReturn(transferProcess("tpId", dataRequest));
        when(statusCheckerRegistry.resolve(any())).thenReturn(null);

        var result = handler.handle(new Complete("tpId"));

        assertThat(result.getNextCommand()).isNull();
        assertThat(result.getPostAction().apply(mock(TransferProcessListener.class))).isNull();
        verify(store, never()).update(any());
    }

    @Test
    void should_transition_process_with_managed_resources_and_status_checker_returns_completed() {
        var dataRequest = DataRequest.Builder.newInstance().managedResources(true).dataDestination(DataAddress.Builder.newInstance().build()).build();
        when(store.find("tpId")).thenReturn(transferProcess("tpId", dataRequest));
        var statusChecker = mock(StatusChecker.class);
        when(statusChecker.isComplete(any(), any())).thenReturn(true);
        when(statusCheckerRegistry.resolve(any())).thenReturn(statusChecker);

        var result = handler.handle(new Complete("tpId"));

        assertThat(result.getNextCommand()).isNull();
        assertThat(result.getPostAction().apply(mock(TransferProcessListener.class))).isNotNull();
        verify(store).update(argThat(process -> process.getState() == COMPLETED.code()));
    }

    @Test
    void should_transition_process_with_non_managed_resources_and_no_status_checker() {
        var dataRequest = DataRequest.Builder.newInstance().managedResources(false).dataDestination(DataAddress.Builder.newInstance().build()).build();
        when(store.find("tpId")).thenReturn(transferProcess("tpId", dataRequest));
        when(statusCheckerRegistry.resolve(any())).thenReturn(null);

        var result = handler.handle(new Complete("tpId"));

        assertThat(result.getNextCommand()).isNull();
        assertThat(result.getPostAction().apply(mock(TransferProcessListener.class))).isNotNull();
        verify(store).update(argThat(process -> process.getState() == COMPLETED.code()));
    }

    @Test
    void should_transition_process_with_non_managed_resources_and_status_checker_returns_completed() {
        var dataRequest = DataRequest.Builder.newInstance().managedResources(false).dataDestination(DataAddress.Builder.newInstance().build()).build();
        when(store.find("tpId")).thenReturn(transferProcess("tpId", dataRequest));
        var statusChecker = mock(StatusChecker.class);
        when(statusChecker.isComplete(any(), any())).thenReturn(true);
        when(statusCheckerRegistry.resolve(any())).thenReturn(statusChecker);

        var result = handler.handle(new Complete("tpId"));

        assertThat(result.getNextCommand()).isNull();
        assertThat(result.getPostAction().apply(mock(TransferProcessListener.class))).isNotNull();
        verify(store).update(argThat(process -> process.getState() == COMPLETED.code()));
    }

    private TransferProcess transferProcess(String id, DataRequest dataRequest) {
        return TransferProcess.Builder.newInstance()
                .id(id)
                .dataRequest(dataRequest)
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
                .state(IN_PROGRESS.code())
                .build();
    }
}