/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.core.provision;

import com.microsoft.dagx.policy.model.AtomicConstraintFunction;
import com.microsoft.dagx.policy.model.Duty;
import com.microsoft.dagx.policy.model.Permission;
import com.microsoft.dagx.policy.model.Prohibition;
import com.microsoft.dagx.spi.transfer.provision.ResourceDefinitionGenerator;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceManifest;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default implemnentation.
 */
public class ResourceManifestGeneratorImpl implements ResourceManifestGenerator {
    private List<ResourceDefinitionGenerator> clientGenerators = new ArrayList<>();
    private List<ResourceDefinitionGenerator> providerGenerators = new ArrayList<>();

    @Override
    public void registerClientGenerator(ResourceDefinitionGenerator generator) {
        clientGenerators.add(generator);
    }

    @Override
    public void registerProviderGenerator(ResourceDefinitionGenerator generator) {
        providerGenerators.add(generator);
    }

    @Override
    public void registerPermissionFunctions(Map<String, AtomicConstraintFunction<String, Permission, Boolean>> functions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerProhibitionFunctions(Map<String, AtomicConstraintFunction<String, Prohibition, Boolean>> functions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerObligationFunctions(Map<String, AtomicConstraintFunction<String, Duty, Boolean>> functions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceManifest generateClientManifest(TransferProcess process) {
        var manifest = new ResourceManifest();
        clientGenerators.forEach(g -> {
            var definition = g.generate(process);
            if (definition != null) {
                manifest.addDefinition(definition);
            }
        });
        return manifest;
    }

    @Override
    public ResourceManifest generateProviderManifest(TransferProcess process) {
        var manifest = new ResourceManifest();
        providerGenerators.forEach(g -> {
            var definition = g.generate(process);
            if (definition != null) {
                manifest.addDefinition(definition);
            }
        });
        return manifest;

    }
}
