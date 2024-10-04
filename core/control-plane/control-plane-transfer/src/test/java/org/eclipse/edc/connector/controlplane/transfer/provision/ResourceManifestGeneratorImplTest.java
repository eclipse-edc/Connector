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

package org.eclipse.edc.connector.controlplane.transfer.provision;

import org.eclipse.edc.connector.controlplane.transfer.TestResourceDefinition;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResourceManifestGeneratorImplTest {

    private final ConsumerResourceDefinitionGenerator consumerGenerator = mock();
    private final ProviderResourceDefinitionGenerator providerGenerator = mock();
    private final PolicyEngine policyEngine = mock();
    private final ResourceManifestGeneratorImpl generator = new ResourceManifestGeneratorImpl(policyEngine);
    private final Policy policy = Policy.Builder.newInstance().build();
    private final DataAddress dataAddress = DataAddress.Builder.newInstance().type("test").build();

    @BeforeEach
    void setUp() {
        generator.registerGenerator(consumerGenerator);
        generator.registerGenerator(providerGenerator);
    }

    @Test
    void shouldGenerateResourceManifestForConsumerManagedTransferProcess() {
        var transferProcess = TransferProcess.Builder.newInstance().dataDestination(dataDestination()).build();
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        when(consumerGenerator.canGenerate(any(), any())).thenReturn(true);
        when(consumerGenerator.generate(any(), any())).thenReturn(resourceDefinition);
        when(policyEngine.evaluate(any(), isA(PolicyContext.class))).thenReturn(Result.success());

        var result = generator.generateConsumerResourceManifest(transferProcess, policy);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getDefinitions()).hasSize(1).containsExactly(resourceDefinition);
        verifyNoInteractions(providerGenerator);
    }

    @Test
    void shouldGenerateEmptyResourceManifestForNotGeneratedFilter() {
        var transferProcess = TransferProcess.Builder.newInstance().dataDestination(dataDestination()).build();
        when(consumerGenerator.canGenerate(any(), any())).thenReturn(false);
        when(policyEngine.evaluate(any(), isA(PolicyContext.class))).thenReturn(Result.success());

        var result = generator.generateConsumerResourceManifest(transferProcess, policy);

        assertThat(result.getContent().getDefinitions()).hasSize(0);
        verifyNoInteractions(providerGenerator);
    }

    @Test
    void shouldReturnFailedResultForConsumerWhenPolicyEvaluationFailed() {
        var transferProcess = TransferProcess.Builder.newInstance().dataDestination(dataDestination()).build();
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        when(consumerGenerator.generate(any(), any())).thenReturn(resourceDefinition);
        when(policyEngine.evaluate(any(), isA(PolicyContext.class))).thenReturn(Result.failure("error"));

        var result = generator.generateConsumerResourceManifest(transferProcess, policy);

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void shouldGenerateResourceManifestForProviderTransferProcess() {
        var transferProcess = TransferProcess.Builder.newInstance().dataDestination(dataDestination()).build();
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        when(providerGenerator.canGenerate(any(), any(), any())).thenReturn(true);
        when(providerGenerator.generate(any(), any(), any())).thenReturn(resourceDefinition);

        var resourceManifest = generator.generateProviderResourceManifest(transferProcess, dataAddress, policy);

        assertThat(resourceManifest.getDefinitions()).hasSize(1).containsExactly(resourceDefinition);
        verifyNoInteractions(consumerGenerator);
    }

    @Test
    void shouldGenerateEmptyResourceManifestForProviderTransferProcess() {
        var transferProcess = TransferProcess.Builder.newInstance().dataDestination(dataDestination()).build();
        when(providerGenerator.canGenerate(any(), any(), any())).thenReturn(false);

        var resourceManifest = generator.generateProviderResourceManifest(transferProcess, dataAddress, policy);

        assertThat(resourceManifest.getDefinitions()).hasSize(0);
        verifyNoInteractions(consumerGenerator);
    }

    private DataAddress dataDestination() {
        return DataAddress.Builder.newInstance().type("any").build();
    }
}
