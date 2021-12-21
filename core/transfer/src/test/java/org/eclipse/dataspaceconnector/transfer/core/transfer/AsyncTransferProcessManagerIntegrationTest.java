package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.DeprovisionResponse;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusChecker;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.transfer.core.TestProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.transfer.core.TestResourceDefinition;
import org.eclipse.dataspaceconnector.transfer.store.memory.InMemoryTransferProcessStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.ENDED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AsyncTransferProcessManagerIntegrationTest {

    private final InMemoryTransferProcessStore store = new InMemoryTransferProcessStore();
    private final ProvisionManager provisionManager = mock(ProvisionManager.class);
    private final ResourceManifestGenerator manifestGenerator = mock(ResourceManifestGenerator.class);
    private final DataFlowManager dataFlowManager = mock(DataFlowManager.class);
    private final StatusCheckerRegistry statusCheckerRegistry = mock(StatusCheckerRegistry.class);
    private AsyncTransferProcessManager transferProcessManager;

    @BeforeEach
    void setUp() {
        transferProcessManager = AsyncTransferProcessManager.Builder.newInstance()
                .manifestGenerator(manifestGenerator)
                .provisionManager(provisionManager)
                .dataFlowManager(dataFlowManager)
                .dispatcherRegistry(mock(RemoteMessageDispatcherRegistry.class))
                .monitor(mock(Monitor.class))
                .statusCheckerRegistry(statusCheckerRegistry)
                .build();

        transferProcessManager.start(store);
    }

    @Test
    void provider_transfer() {
        var resourceDefinitionId = randomUUID().toString();
        when(manifestGenerator.generateProviderManifest(any())).thenAnswer(manifest(resourceDefinitionId));
        when(provisionManager.provision(any())).thenAnswer(provisionResponse(resourceDefinitionId));
        when(provisionManager.deprovision(any())).thenAnswer(deprovisionResponse(resourceDefinitionId));
        when(dataFlowManager.initiate(any())).thenReturn(DataFlowInitiateResult.success("something"));
        StatusChecker statusChecker = mock(StatusChecker.class);
        when(statusChecker.isComplete(any(), any())).thenReturn(true);
        when(statusCheckerRegistry.resolve(any())).thenReturn(statusChecker);
        DataRequest dataRequest = DataRequest.Builder.newInstance().dataDestination(DataAddress.Builder.newInstance().build()).id("dataRequestId").build();

        String processId = transferProcessManager.initiateProviderRequest(dataRequest).getContent();

        assertStateIs(processId, COMPLETED);

        transferProcessManager.deprovision(processId);

        assertStateIs(processId, ENDED);
    }

    @Test
    void consumer_transfer_managed_resources() {
        var resourceDefinitionId = randomUUID().toString();
        when(manifestGenerator.generateConsumerManifest(any())).thenAnswer(manifest(resourceDefinitionId));
        when(provisionManager.provision(any())).thenAnswer(provisionResponse(resourceDefinitionId));
        when(provisionManager.deprovision(any())).thenAnswer(deprovisionResponse(resourceDefinitionId));
        StatusChecker statusChecker = mock(StatusChecker.class);
        when(statusChecker.isComplete(any(), any())).thenReturn(true);
        when(statusCheckerRegistry.resolve(any())).thenReturn(statusChecker);
        DataRequest dataRequest = DataRequest.Builder.newInstance().dataDestination(DataAddress.Builder.newInstance().build()).id("dataRequestId").build();

        String processId = transferProcessManager.initiateConsumerRequest(dataRequest).getContent();

        assertStateIs(processId, REQUESTED);

        transferProcessManager.transitionRequestAck(processId).join();

        assertStateIs(processId, COMPLETED);

        transferProcessManager.deprovision(processId);

        assertStateIs(processId, ENDED);
    }

    @Test
    void consumer_transfer_not_managed_resources() {
        var resourceDefinitionId = randomUUID().toString();
        when(manifestGenerator.generateConsumerManifest(any())).thenAnswer(manifest(resourceDefinitionId));
        when(provisionManager.provision(any())).thenAnswer(provisionResponse(resourceDefinitionId));
        when(provisionManager.deprovision(any())).thenAnswer(deprovisionResponse(resourceDefinitionId));
        when(statusCheckerRegistry.resolve(any())).thenReturn(null);
        DataRequest dataRequest = DataRequest.Builder.newInstance().dataDestination(DataAddress.Builder.newInstance().build()).id("dataRequestId").managedResources(false).build();

        String processId = transferProcessManager.initiateConsumerRequest(dataRequest).getContent();

        assertStateIs(processId, REQUESTED);

        transferProcessManager.transitionRequestAck(processId).join();

        assertStateIs(processId, COMPLETED);

        transferProcessManager.deprovision(processId);

        assertStateIs(processId, ENDED);
    }

    @NotNull
    private Answer<Object> manifest(String resourceDefinitionId) {
        return i -> {
            var process = i.getArgument(0, TransferProcess.class);
            var resourceDefinition = TestResourceDefinition.Builder.newInstance()
                    .id(resourceDefinitionId)
                    .transferProcessId(process.getId())
                    .build();
            return ResourceManifest.Builder.newInstance().definitions(List.of(resourceDefinition)).build();
        };
    }

    @NotNull
    private Answer<Object> provisionResponse(String resourceDefinitionId) {
        return i -> {
            var process = i.getArgument(0, TransferProcess.class);
            var resourceDefinition = TestProvisionedDataDestinationResource.Builder.newInstance()
                    .id(randomUUID().toString()).transferProcessId(process.getId())
                    .resourceDefinitionId(resourceDefinitionId)
                    .build();
            var provisionResponse = ProvisionResponse.Builder.newInstance().resource(resourceDefinition).build();
            return List.of(CompletableFuture.completedFuture(provisionResponse));
        };
    }

    @NotNull
    private Answer<Object> deprovisionResponse(String resourceDefinitionId) {
        return i -> {
            var process = i.getArgument(0, TransferProcess.class);
            var resourceDefinition = TestProvisionedDataDestinationResource.Builder.newInstance()
                    .id(randomUUID().toString()).transferProcessId(process.getId())
                    .resourceDefinitionId(resourceDefinitionId)
                    .build();
            var provisionResponse = DeprovisionResponse.Builder.newInstance().ok().resource(resourceDefinition).build();
            return List.of(CompletableFuture.completedFuture(provisionResponse));
        };
    }

    private void assertStateIs(String processId, TransferProcessStates state) {
        await().untilAsserted(() -> {
            TransferProcess actual = store.find(processId);
            assertThat(actual).matches(it -> it.getState() == state.code());
        });
    }

}
