package org.eclipse.dataspaceconnector.transfer.core.provision;

<<<<<<< HEAD
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.transfer.provision.DeprovisionResponse;
=======
>>>>>>> 663912a81 (spi: make provisioner async)
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
<<<<<<< HEAD
import static java.util.concurrent.TimeUnit.SECONDS;
=======
>>>>>>> 663912a81 (spi: make provisioner async)
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProvisionManagerImplTest {

    private final ProvisionManagerImpl provisionManager = new ProvisionManagerImpl();
    private final Provisioner provisioner = mock(Provisioner.class);

<<<<<<< HEAD
    @BeforeEach
    void setUp() {
        provisionManager.register(provisioner);
    }

=======
>>>>>>> 663912a81 (spi: make provisioner async)
    @Test
    void provision_transfer_process() {
        when(provisioner.canProvision(isA(TestResourceDefinition.class))).thenReturn(true);
        var provisionResponse = ProvisionResponse.Builder.newInstance()
                .resource(new TestProvisionedDataDestinationResource("test-resource"))
                .build();
        when(provisioner.provision(isA(TestResourceDefinition.class))).thenReturn(completedFuture(provisionResponse));
<<<<<<< HEAD
        TransferProcess transferProcess = TransferProcess.Builder.newInstance()
                .id("id")
                .state(TransferProcessStates.REQUESTED.code())
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();

        var result = provisionManager.provision(transferProcess);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).succeedsWithin(1, SECONDS)
                .extracting(ProvisionResponse::getResource)
                .extracting(ProvisionedDataDestinationResource.class::cast)
                .extracting(ProvisionedDataDestinationResource::getResourceName)
                .isEqualTo("test-resource");
    }

    @Test
    void should_handle_provisioner_exception() {
        when(provisioner.canProvision(isA(TestResourceDefinition.class))).thenReturn(true);
        when(provisioner.provision(isA(TestResourceDefinition.class))).thenThrow(new EdcException("error"));
=======
        provisionManager.register(provisioner);
>>>>>>> 663912a81 (spi: make provisioner async)
        TransferProcess transferProcess = TransferProcess.Builder.newInstance()
                .id("id")
                .state(TransferProcessStates.REQUESTED.code())
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();

        var result = provisionManager.provision(transferProcess);

        assertThat(result).hasSize(1);
<<<<<<< HEAD
        assertThat(result.get(0)).failsWithin(1, SECONDS);
=======
        assertThat(result.get(0).join().getResource().getResourceName()).isEqualTo("test-resource");
>>>>>>> 663912a81 (spi: make provisioner async)
    }

    @Test
    void deprovision_transfer_process_returns_response_list() {
        var deprovisionResponse = DeprovisionResponse.Builder.newInstance()
                .ok()
                .resource(new TestProvisionedDataDestinationResource("test-resource"))
                .build();
        when(provisioner.canDeprovision(isA(ProvisionedResource.class))).thenReturn(true);
<<<<<<< HEAD
        when(provisioner.deprovision(isA(TestProvisionedResource.class))).thenReturn(completedFuture(deprovisionResponse));
=======
        when(provisioner.deprovision(isA(TestProvisionedResource.class))).thenReturn(completedFuture(ResponseStatus.OK));
        provisionManager.register(provisioner);
>>>>>>> 663912a81 (spi: make provisioner async)
        TransferProcess transferProcess = TransferProcess.Builder.newInstance()
                .id("id")
                .state(TransferProcessStates.REQUESTED.code())
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().resources(List.of(new TestProvisionedResource())).build())
                .build();

        var result = provisionManager.deprovision(transferProcess);

        assertThat(result).hasSize(1);
<<<<<<< HEAD
        assertThat(result.get(0)).succeedsWithin(1, SECONDS)
                .extracting(DeprovisionResponse::getStatus)
                .isEqualTo(ResponseStatus.OK);
    }

    @Test
    void should_handle_deprovision_exception() {
        when(provisioner.canDeprovision(isA(ProvisionedResource.class))).thenReturn(true);
        when(provisioner.deprovision(isA(TestProvisionedResource.class))).thenThrow(new EdcException("error"));
        TransferProcess transferProcess = TransferProcess.Builder.newInstance()
                .id("id")
                .state(TransferProcessStates.REQUESTED.code())
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().resources(List.of(new TestProvisionedResource())).build())
                .build();

        var result = provisionManager.deprovision(transferProcess);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).failsWithin(1, SECONDS);
=======
        assertThat(result.get(0).join()).isEqualTo(ResponseStatus.OK);
>>>>>>> 663912a81 (spi: make provisioner async)
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