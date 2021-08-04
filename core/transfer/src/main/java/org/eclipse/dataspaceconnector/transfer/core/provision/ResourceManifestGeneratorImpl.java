/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.provision;

import org.eclipse.dataspaceconnector.policy.model.AtomicConstraintFunction;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default implementation.
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
