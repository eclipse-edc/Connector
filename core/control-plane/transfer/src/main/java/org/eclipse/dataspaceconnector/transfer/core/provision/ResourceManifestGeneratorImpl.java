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

package org.eclipse.dataspaceconnector.transfer.core.provision;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.policy.engine.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        if (!dataRequest.isManagedResources()) {
            return Result.success(ResourceManifest.Builder.newInstance().build());
        }
        var definitions = consumerGenerators.stream()
                .map(generator -> generator.generate(dataRequest, policy))
                .filter(Objects::nonNull).collect(Collectors.toList());
    
        var manifest = ResourceManifest.Builder.newInstance().definitions(definitions).build();
        
        // Make all definitions available through manifest context
        var manifestContext = new ResourceManifestContext(manifest);
        
        // Create additional context information for policy engine to make manifest context available
        var contextInformation = new HashMap<Class, Object>();
        contextInformation.put(ResourceManifestContext.class, manifestContext);
    
        var result = policyEngine.evaluate(MANIFEST_VERIFICATION_SCOPE, policy, null, contextInformation);
        if (result.succeeded()) {
            return Result.success(buildFromContextInformation(contextInformation));
        } else {
            return Result.failure(result.getFailureMessages());
        }
    }
    
    private ResourceManifest buildFromContextInformation(Map<Class, Object> contextInformation) {
        var manifestContext = (ResourceManifestContext) contextInformation.get(ResourceManifestContext.class);
        return ResourceManifest.Builder.newInstance().definitions(manifestContext.getDefinitions()).build();
    }

    @Override
    public ResourceManifest generateProviderResourceManifest(DataRequest dataRequest, DataAddress assetAddress, Policy policy) {
        var definitions = providerGenerators.stream()
                .map(generator -> generator.generate(dataRequest, assetAddress, policy))
                .filter(Objects::nonNull).collect(Collectors.toList());

        return ResourceManifest.Builder.newInstance().definitions(definitions).build();
    }

}
