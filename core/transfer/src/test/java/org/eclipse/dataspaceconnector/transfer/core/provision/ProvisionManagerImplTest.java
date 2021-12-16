package org.eclipse.dataspaceconnector.transfer.core.provision;

import org.eclipse.dataspaceconnector.spi.transfer.provision.DeprovisionResponse;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResourceSet;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.transfer.core.TestResourceDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProvisionManagerImplTest {

    private final ProvisionManagerImpl provisionManager = new ProvisionManagerImpl();
    private final Provisioner provisioner = mock(Provisioner.class);

    @BeforeEach
    void setUp() {
        provisionManager.register(provisioner);
    }

    @Test
    void provisionTransferProcess() {
        when(provisioner.canProvision(isA(TestResourceDefinition.class))).thenReturn(true);
        var provisionResponse = ProvisionResponse.Builder.newInstance()
                .resource(new TestProvisionedDataDestinationResource("test-resource"))
                .build();
        when(provisioner.provision(isA(TestResourceDefinition.class))).thenReturn(completedFuture(provisionResponse));
        TransferProcess transferProcess = TransferProcess.Builder.newInstance()
                .id("id")
                .state(TransferProcessStates.REQUESTED.code())
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        provisionManager.start(mock(ProvisionContext.class));

        var result = provisionManager.provision(transferProcess);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).join().getResource().getResourceName()).isEqualTo("test-resource");
    }

    @Test
    void deprovisionTransferProcessReturnsResponseList() {
        var deprovisionResponse = DeprovisionResponse.Builder.newInstance()
                .ok()
                .resource(new TestProvisionedDataDestinationResource("test-resource"))
                .build();
        when(provisioner.canDeprovision(isA(ProvisionedResource.class))).thenReturn(true);
        when(provisioner.deprovision(isA(TestProvisionedResource.class))).thenReturn(completedFuture(deprovisionResponse));
        TransferProcess transferProcess = TransferProcess.Builder.newInstance()
                .id("id")
                .state(TransferProcessStates.REQUESTED.code())
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().resources(List.of(new TestProvisionedResource())).build())
                .build();
        provisionManager.start(mock(ProvisionContext.class));

        var result = provisionManager.deprovision(transferProcess);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).join().getStatus()).isEqualTo(ResponseStatus.OK);
    }

    private static class TestProvisionedResource extends ProvisionedResource {}

    private static class TestProvisionedDataDestinationResource extends ProvisionedDataDestinationResource {
        private final String resourceName;

        protected TestProvisionedDataDestinationResource(String resourceName) {
            super();
            this.resourceName = resourceName;
        }

        @Override
        public DataAddress createDataDestination() {
            return null;
        }

        @Override
        public String getResourceName() {
            return resourceName;
        }
    }
}