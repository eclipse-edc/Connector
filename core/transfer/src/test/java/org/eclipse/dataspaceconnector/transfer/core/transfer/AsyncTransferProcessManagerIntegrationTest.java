package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusChecker;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.TestProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.transfer.core.TestResourceDefinition;
import org.eclipse.dataspaceconnector.transfer.core.provision.ProvisionManagerImpl;
import org.eclipse.dataspaceconnector.transfer.store.memory.InMemoryTransferProcessStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.IN_PROGRESS;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    void provider_request_provisioning() {
        var resourceDefinitionId = randomUUID().toString();
        when(manifestGenerator.generateProviderManifest(any())).thenAnswer(i -> {
            var process = i.getArgument(0, TransferProcess.class);
            var resourceDefinition = TestResourceDefinition.Builder.newInstance().id(resourceDefinitionId).transferProcessId(process.getId()).build();
            return ResourceManifest.Builder.newInstance().definitions(List.of(resourceDefinition)).build();
        });
        when(provisionManager.provision(any())).thenAnswer(i -> {
            var process = i.getArgument(0, TransferProcess.class);
            var resourceDefinition = TestProvisionedDataDestinationResource.Builder.newInstance()
                    .id(randomUUID().toString()).transferProcessId(process.getId())
                    .resourceDefinitionId(resourceDefinitionId)
                    .build();
            var provisionResponse = ProvisionResponse.Builder.newInstance().resource(resourceDefinition).build();
            return List.of(CompletableFuture.completedFuture(provisionResponse));
        });
        when(dataFlowManager.initiate(any())).thenReturn(DataFlowInitiateResult.success("something"));
        StatusChecker statusChecker = mock(StatusChecker.class);
        when(statusChecker.isComplete(any(), any())).thenReturn(true);
        when(statusCheckerRegistry.resolve(any())).thenReturn(statusChecker);
        DataRequest dataRequest = DataRequest.Builder.newInstance().dataDestination(DataAddress.Builder.newInstance().build()).id("dataRequestId").build();

        TransferInitiateResult result = transferProcessManager.initiateProviderRequest(dataRequest);

        await().untilAsserted(() -> {
            TransferProcess actual = store.find(result.getContent());
            assertThat(actual).matches(it -> it.getState() == COMPLETED.code());
        });
    }

    @Test
    void consumer_request_provisioning() {
        DataRequest dataRequest = DataRequest.Builder.newInstance().dataDestination(DataAddress.Builder.newInstance().build()).id("dataRequestId").build();

        TransferInitiateResult result = transferProcessManager.initiateConsumerRequest(dataRequest);

        await().untilAsserted(() -> {
            TransferProcess actual = store.find(result.getContent());
            assertThat(actual).matches(it -> it.getState() == IN_PROGRESS.code());
        });
    }

}
