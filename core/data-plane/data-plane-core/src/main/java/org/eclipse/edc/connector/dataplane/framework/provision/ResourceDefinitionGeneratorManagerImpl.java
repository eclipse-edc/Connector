/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.framework.provision;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGenerator;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class ResourceDefinitionGeneratorManagerImpl implements ResourceDefinitionGeneratorManager {

    private final List<ResourceDefinitionGenerator> consumerGenerators = new ArrayList<>();
    private final List<ResourceDefinitionGenerator> providerGenerators = new ArrayList<>();

    @Override
    public void registerConsumerGenerator(ResourceDefinitionGenerator generator) {
        consumerGenerators.add(generator);
    }

    @Override
    public void registerProviderGenerator(ResourceDefinitionGenerator generator) {
        providerGenerators.add(generator);
    }

    @Override
    public List<ProvisionResource> generateConsumerResourceDefinition(DataFlow dataFlow) {
        return consumerGenerators.stream()
                .filter(g -> g.supportedType().equals(dataFlow.getDestination().getType()))
                .map(g -> g.generate(dataFlow))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<ProvisionResource> generateProviderResourceDefinition(DataFlow dataFlow) {
        return providerGenerators.stream()
                .filter(g -> g.supportedType().equals(dataFlow.getDestination().getType()))
                .map(g -> g.generate(dataFlow))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Set<String> destinationTypes() {
        return consumerGenerators.stream().map(ResourceDefinitionGenerator::supportedType).collect(toSet());
    }
}
