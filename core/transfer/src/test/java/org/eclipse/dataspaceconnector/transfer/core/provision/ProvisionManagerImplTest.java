package org.eclipse.dataspaceconnector.transfer.core.provision;

import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResourceSet;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.transfer.core.TestResourceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProvisionManagerImplTest {

    private final ProvisionManagerImpl provisionManager = new ProvisionManagerImpl();

    @BeforeEach
    void setUp() {
    }

    @Test
    void provisionTransferProcess() {
        var provisioner = mock(Provisioner.class);
        when(provisioner.canProvision(isA(TestResourceDefinition.class))).thenReturn(true);
        provisionManager.register(provisioner);
        TransferProcess transferProcess = TransferProcess.Builder.newInstance()
                .id("id")
                .state(TransferProcessStates.REQUESTED.code())
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        provisionManager.start(mock(ProvisionContext.class));

        provisionManager.provision(transferProcess);

        verify(provisioner).provision(any());
    }

    @Test
    void deprovisionTransferProcessReturnsResponseList() {
        var provisioner = mock(Provisioner.class);
        when(provisioner.canDeprovision(isA(ProvisionedResource.class))).thenReturn(true);
        when(provisioner.deprovision(isA(TestProvisionedResource.class))).thenReturn(ResponseStatus.OK);
        provisionManager.register(provisioner);
        TransferProcess transferProcess = TransferProcess.Builder.newInstance()
                .id("id")
                .state(TransferProcessStates.REQUESTED.code())
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().resources(List.of(new TestProvisionedResource())).build())
                .build();
        provisionManager.start(mock(ProvisionContext.class));

        List<ResponseStatus> status = provisionManager.deprovision(transferProcess);

        assertThat(status).containsExactly(ResponseStatus.OK);
        verify(provisioner).deprovision(any());
    }

    private static class TestProvisionedResource extends ProvisionedResource {
    }
}