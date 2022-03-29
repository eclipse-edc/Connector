package org.eclipse.dataspaceconnector.transfer.core.provision;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.TestResourceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.CONSUMER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.PROVIDER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResourceManifestGeneratorImplTest {

    private final ResourceDefinitionGenerator consumerGenerator = mock(ResourceDefinitionGenerator.class);
    private final ResourceDefinitionGenerator providerGenerator = mock(ResourceDefinitionGenerator.class);
    private final ResourceManifestGeneratorImpl generator = new ResourceManifestGeneratorImpl();
    private Policy policy;

    @BeforeEach
    void setUp() {
        generator.registerConsumerGenerator(consumerGenerator);
        generator.registerProviderGenerator(providerGenerator);
        policy = Policy.Builder.newInstance().build();
    }

    @Test
    void shouldGenerateResourceManifestForConsumerManagedTransferProcess() {
        var process = createTransferProcess(CONSUMER, true);
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        when(consumerGenerator.generate(any(), any())).thenReturn(resourceDefinition);

        var resourceManifest = generator.generateResourceManifest(process, policy);

        assertThat(resourceManifest.getDefinitions()).hasSize(1).containsExactly(resourceDefinition);
        verifyNoInteractions(providerGenerator);
    }

    @Test
    void shouldGenerateEmptyResourceManifestForEmptyConsumerNotManagedTransferProcess() {
        var process = createTransferProcess(CONSUMER, false);

        var resourceManifest = generator.generateResourceManifest(process, policy);

        assertThat(resourceManifest.getDefinitions()).isEmpty();
        verifyNoInteractions(consumerGenerator);
        verifyNoInteractions(providerGenerator);
    }

    @Test
    void shouldGenerateResourceManifestForProviderTransferProcess() {
        var process = createTransferProcess(PROVIDER, false);
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        when(providerGenerator.generate(any(), any())).thenReturn(resourceDefinition);

        var resourceManifest = generator.generateResourceManifest(process, policy);

        assertThat(resourceManifest.getDefinitions()).hasSize(1).containsExactly(resourceDefinition);
        verifyNoInteractions(consumerGenerator);
    }

    private TransferProcess createTransferProcess(TransferProcess.Type type, boolean managedResources) {
        var destination = DataAddress.Builder.newInstance().type("any").build();
        var dataRequest = DataRequest.Builder.newInstance().managedResources(managedResources).dataDestination(destination).build();
        return TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).dataRequest(dataRequest).type(type).build();
    }
}
