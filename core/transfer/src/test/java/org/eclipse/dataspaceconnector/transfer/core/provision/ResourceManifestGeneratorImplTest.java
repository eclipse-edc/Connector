/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.provision;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.core.TestResourceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResourceManifestGeneratorImplTest {

    private final ConsumerResourceDefinitionGenerator consumerGenerator = mock(ConsumerResourceDefinitionGenerator.class);
    private final ProviderResourceDefinitionGenerator providerGenerator = mock(ProviderResourceDefinitionGenerator.class);
    private final ResourceManifestGeneratorImpl generator = new ResourceManifestGeneratorImpl();
    private Policy policy;
    private DataAddress dataAddress;

    @BeforeEach
    void setUp() {
        generator.registerGenerator(consumerGenerator);
        generator.registerGenerator(providerGenerator);
        policy = Policy.Builder.newInstance().build();
        dataAddress = DataAddress.Builder.newInstance().type("test").build();
    }

    @Test
    void shouldGenerateResourceManifestForConsumerManagedTransferProcess() {
        var dataRequest = createDataRequest(true);
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        when(consumerGenerator.generate(any(), any())).thenReturn(resourceDefinition);

        var resourceManifest = generator.generateConsumerResourceManifest(dataRequest, policy);

        assertThat(resourceManifest.getDefinitions()).hasSize(1).containsExactly(resourceDefinition);
        verifyNoInteractions(providerGenerator);
    }

    @Test
    void shouldGenerateEmptyResourceManifestForEmptyConsumerNotManagedTransferProcess() {
        var dataRequest = createDataRequest(false);

        var resourceManifest = generator.generateConsumerResourceManifest(dataRequest, policy);

        assertThat(resourceManifest.getDefinitions()).isEmpty();
        verifyNoInteractions(consumerGenerator);
        verifyNoInteractions(providerGenerator);
    }

    @Test
    void shouldGenerateResourceManifestForProviderTransferProcess() {
        var process = createDataRequest(false);
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        when(providerGenerator.generate(any(), any(), any())).thenReturn(resourceDefinition);

        var resourceManifest = generator.generateProviderResourceManifest(process, dataAddress, policy);

        assertThat(resourceManifest.getDefinitions()).hasSize(1).containsExactly(resourceDefinition);
        verifyNoInteractions(consumerGenerator);
    }

    private DataRequest createDataRequest(boolean managedResources) {
        var destination = DataAddress.Builder.newInstance().type("any").build();
        return DataRequest.Builder.newInstance().managedResources(managedResources).dataDestination(destination).build();
    }
}
