/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - policy evaluation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.provision;

import org.eclipse.edc.connector.controlplane.transfer.spi.policy.ProvisionManifestVerifyPolicyContext;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ResourceManifestContext;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default implementation.
 */
public class ResourceManifestGeneratorImpl implements ResourceManifestGenerator {

    private final List<ConsumerResourceDefinitionGenerator> consumerGenerators = new ArrayList<>();
    private final List<ProviderResourceDefinitionGenerator> providerGenerators = new ArrayList<>();
    private final PolicyEngine policyEngine;

    public ResourceManifestGeneratorImpl(PolicyEngine policyEngine) {
        this.policyEngine = policyEngine;
    }

    @Override
    public void registerGenerator(ConsumerResourceDefinitionGenerator generator) {
        consumerGenerators.add(generator);
    }

    @Override
    public void registerGenerator(ProviderResourceDefinitionGenerator generator) {
        providerGenerators.add(generator);
    }

    @Override
    public Result<ResourceManifest> generateConsumerResourceManifest(TransferProcess transferProcess, Policy policy) {
        var definitions = consumerGenerators.stream()
                .filter(generator -> generator.canGenerate(transferProcess, policy))
                .map(generator -> generator.generate(transferProcess, policy))
                .filter(Objects::nonNull).collect(Collectors.toList());

        var manifest = ResourceManifest.Builder.newInstance().definitions(definitions).build();

        // Make all definitions available through manifest context
        var manifestContext = new ResourceManifestContext(manifest);

        // Create additional context information for policy engine to make manifest context available
        var policyContext = new ProvisionManifestVerifyPolicyContext(manifestContext);

        return policyEngine.evaluate(policy, policyContext)
                .map(a -> ResourceManifest.Builder.newInstance()
                        .definitions(policyContext.resourceManifestContext().getDefinitions())
                        .build()
                );
    }

    @Override
    public ResourceManifest generateProviderResourceManifest(TransferProcess transferProcess, DataAddress assetAddress, Policy policy) {
        var definitions = providerGenerators.stream()
                .filter(generator -> generator.canGenerate(transferProcess, assetAddress, policy))
                .map(generator -> generator.generate(transferProcess, assetAddress, policy))
                .filter(Objects::nonNull).collect(Collectors.toList());

        return ResourceManifest.Builder.newInstance().definitions(definitions).build();
    }

}
