package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.provision.ProvisionManagerImpl;
import org.eclipse.dataspaceconnector.transfer.store.memory.InMemoryTransferProcessStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.IN_PROGRESS;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONING;
import static org.mockito.Mockito.mock;

public class AsyncTransferProcessManagerIntegrationTest {

    private final InMemoryTransferProcessStore store = new InMemoryTransferProcessStore();
    private final ProvisionManagerImpl provisionManager = new ProvisionManagerImpl();
    private AsyncTransferProcessManager transferProcessManager;

    @BeforeEach
    void setUp() {
        transferProcessManager = AsyncTransferProcessManager.Builder.newInstance()
                .manifestGenerator(mock(ResourceManifestGenerator.class))
                .provisionManager(provisionManager)
                .dataFlowManager(mock(DataFlowManager.class))
                .dispatcherRegistry(mock(RemoteMessageDispatcherRegistry.class))
                .monitor(mock(Monitor.class))
                .statusCheckerRegistry(mock(StatusCheckerRegistry.class))
                .build();

        transferProcessManager.start(store);
        provisionManager.register(new TestProvisioner());
        provisionManager.start(transferProcessManager.createProvisionContext());
    }

    @Test
    void provider_request_provisioning() {
        DataRequest dataRequest = DataRequest.Builder.newInstance().dataDestination(DataAddress.Builder.newInstance().build()).id("dataRequestId").build();

        TransferInitiateResult result = transferProcessManager.initiateProviderRequest(dataRequest);

        await().untilAsserted(() -> {
            TransferProcess actual = store.find(result.getContent());
            assertThat(actual).matches(it -> it.getState() == IN_PROGRESS.code());
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

    private class TestProvisioner implements Provisioner<ResourceDefinition, ProvisionedDataDestinationResource> {

        private ProvisionContext context;

        @Override
        public void initialize(ProvisionContext context) {
            this.context = context;
        }

        @Override
        public boolean canProvision(ResourceDefinition resourceDefinition) {
            return true;
        }

        @Override
        public boolean canDeprovision(ProvisionedResource resourceDefinition) {
            return true;
        }

        @Override
        public ResponseStatus provision(ResourceDefinition resourceDefinition) {
            context.callback(new TestResource());
            return ResponseStatus.OK;
        }

        @Override
        public ResponseStatus deprovision(ProvisionedDataDestinationResource provisionedResource) {
            context.deprovisioned(new TestResource(), null);
            return ResponseStatus.OK;
        }
    }

    private static class TestResource extends ProvisionedDataDestinationResource {
        protected TestResource() {
            super();
        }

        @Override
        public DataAddress createDataDestination() {
            return null;
        }

        @Override
        public String getResourceName() {
            return "test-resource";
        }
    }
}
