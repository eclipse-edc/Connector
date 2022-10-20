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

package org.eclipse.edc.connector.transfer.provision;

import org.eclipse.edc.connector.transfer.TestResourceDefinition;
import org.eclipse.edc.connector.transfer.spi.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
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
    private ResourceManifestGeneratorImpl generator;
    private Policy policy;
    private DataAddress dataAddress;
    private PolicyEngine policyEngine;

    @BeforeEach
    void setUp() {
        policyEngine = mock(PolicyEngine.class);
        generator = new ResourceManifestGeneratorImpl(policyEngine);
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
        when(policyEngine.evaluate(any(), any(), any(), any())).thenReturn(Result.success(policy));

        var result = generator.generateConsumerResourceManifest(dataRequest, policy);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getDefinitions()).hasSize(1).containsExactly(resourceDefinition);
        verifyNoInteractions(providerGenerator);
    }

    @Test
    void shouldGenerateEmptyResourceManifestForEmptyConsumerNotManagedTransferProcess() {
        var dataRequest = createDataRequest(false);

        var result = generator.generateConsumerResourceManifest(dataRequest, policy);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getDefinitions()).isEmpty();
        verifyNoInteractions(consumerGenerator);
        verifyNoInteractions(providerGenerator);
    }

    @Test
    void shouldReturnFailedResultForConsumerWhenPolicyEvaluationFailed() {
        var dataRequest = createDataRequest(true);
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        when(consumerGenerator.generate(any(), any())).thenReturn(resourceDefinition);
        when(policyEngine.evaluate(any(), any(), any(), any())).thenReturn(Result.failure("error"));

        var result = generator.generateConsumerResourceManifest(dataRequest, policy);

        assertThat(result.succeeded()).isFalse();
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
