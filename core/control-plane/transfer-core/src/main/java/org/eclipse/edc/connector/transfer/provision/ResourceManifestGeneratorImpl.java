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
 *       Fraunhofer Institute for Software and Systems Engineering - policy evaluation
 *
 */

package org.eclipse.edc.connector.transfer.provision;

import org.eclipse.edc.connector.transfer.spi.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestContext;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
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
    public Result<ResourceManifest> generateConsumerResourceManifest(DataRequest dataRequest, Policy policy) {
        var definitions = consumerGenerators.stream()
                .filter(generator -> generator.canGenerate(dataRequest, policy))
                .map(generator -> generator.generate(dataRequest, policy))
                .filter(Objects::nonNull).collect(Collectors.toList());

        var manifest = ResourceManifest.Builder.newInstance().definitions(definitions).build();

        // Make all definitions available through manifest context
        var manifestContext = new ResourceManifestContext(manifest);

        // Create additional context information for policy engine to make manifest context available
        var policyContext = PolicyContextImpl.Builder.newInstance()
                .additional(ResourceManifestContext.class, manifestContext)
                .build();

        return policyEngine.evaluate(MANIFEST_VERIFICATION_SCOPE, policy, policyContext)
                .map(a -> extractModifiedManifest(policyContext));
    }

    @Override
    public ResourceManifest generateProviderResourceManifest(DataRequest dataRequest, DataAddress assetAddress, Policy policy) {
        var definitions = providerGenerators.stream()
                .filter(generator -> generator.canGenerate(dataRequest, assetAddress, policy))
                .map(generator -> generator.generate(dataRequest, assetAddress, policy))
                .filter(Objects::nonNull).collect(Collectors.toList());

        return ResourceManifest.Builder.newInstance().definitions(definitions).build();
    }

    private ResourceManifest extractModifiedManifest(PolicyContext policyContext) {
        var manifestContext = policyContext.getContextData(ResourceManifestContext.class);
        return ResourceManifest.Builder.newInstance().definitions(manifestContext.getDefinitions()).build();
    }
}
