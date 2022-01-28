package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.core.base.ExponentialWaitStrategy;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.DataProxyManager;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResourceSet;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.eclipse.dataspaceconnector.transfer.core.TestProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.transfer.core.TestResourceDefinition;
import org.eclipse.dataspaceconnector.transfer.store.memory.InMemoryTransferProcessStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.UNSAVED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferProcessManagerImplIntegrationTest {

    private static final int TRANSFER_MANAGER_BATCHSIZE = 10;
    private final ProvisionManager provisionManager = mock(ProvisionManager.class);
    private final ResourceManifestGenerator manifestGenerator = mock(ResourceManifestGenerator.class);
    private final TransferProcessStore store = new InMemoryTransferProcessStore();
    private TransferProcessManagerImpl transferProcessManager;

    @BeforeEach
    void setup() {
        var resourceManifest = ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build();
        when(manifestGenerator.generateConsumerManifest(any(TransferProcess.class))).thenReturn(resourceManifest);

        transferProcessManager = TransferProcessManagerImpl.Builder.newInstance()
                .provisionManager(provisionManager)
                .dataFlowManager(mock(DataFlowManager.class))
                .waitStrategy(mock(ExponentialWaitStrategy.class))
                .batchSize(TRANSFER_MANAGER_BATCHSIZE)
                .dispatcherRegistry(mock(RemoteMessageDispatcherRegistry.class))
                .manifestGenerator(manifestGenerator)
                .monitor(mock(Monitor.class))
                .commandQueue(mock(CommandQueue.class))
                .commandRunner(mock(CommandRunner.class))
                .typeManager(new TypeManager())
                .statusCheckerRegistry(mock(StatusCheckerRegistry.class))
                .dataProxyManager(mock(DataProxyManager.class))
                .proxyEntryHandlerRegistry(new ProxyEntryHandlerRegistryImpl())
                .build();

        transferProcessManager.start(store);
    }

    @Test
    @DisplayName("Verify that no process 'starves' during two consecutive runs, when the batch size > number of processes")
    void verifyProvision_shouldNotStarve() throws InterruptedException {
        var numProcesses = TRANSFER_MANAGER_BATCHSIZE * 2;
        var processesToProvision = new CountDownLatch(numProcesses); //all processes should be provisioned
        doAnswer(i -> {
            processesToProvision.countDown();
            return null;
        }).when(provisionManager).provision(any(TransferProcess.class));

        var processes = IntStream.range(0, numProcesses)
                .mapToObj(i -> provisionedResourceSet())
                .map(resourceSet -> createUnsavedTransferProcess().provisionedResourceSet(resourceSet).build())
                .peek(TransferProcess::transitionInitial)
                .peek(store::create)
                .collect(Collectors.toList());

        assertThat(processesToProvision.await(5, SECONDS)).isTrue();
        assertThat(processes).describedAs("All transfer processes should be in PROVISIONING state")
                .allSatisfy(process -> {
                    var id = process.getId();
                    var storedProcess = store.find(id);
                    assertThat(storedProcess).describedAs("Should exist in the TransferProcessStore").isNotNull();
                    assertThat(storedProcess.getState()).isEqualTo(TransferProcessStates.PROVISIONING.code());
                });
        verify(provisionManager, times(numProcesses)).provision(any());
    }

    private ProvisionedResourceSet provisionedResourceSet() {
        return ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(new TestProvisionedDataDestinationResource("test-resource")))
                .build();
    }

    private TransferProcess.Builder createUnsavedTransferProcess() {
        String processId = UUID.randomUUID().toString();
        var dataRequest = DataRequest.Builder.newInstance()
                .id(processId)
                .transferType(new TransferType())
                .managedResources(true)
                .destinationType("test-type")
                .build();

        return TransferProcess.Builder.newInstance()
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
                .type(TransferProcess.Type.CONSUMER)
                .id("test-process-" + processId)
                .state(UNSAVED.code())
                .dataRequest(dataRequest);
    }


}