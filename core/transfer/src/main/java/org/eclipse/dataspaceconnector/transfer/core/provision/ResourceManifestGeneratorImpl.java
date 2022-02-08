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
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.provision;

import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.CONSUMER;

/**
 * Default implementation.
 */
public class ResourceManifestGeneratorImpl implements ResourceManifestGenerator {
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
    public ResourceManifest generateResourceManifest(TransferProcess process) {
        var definitions = generateDefinitions(process);
        return ResourceManifest.Builder.newInstance().definitions(definitions).build();
    }

    @NotNull
    private List<ResourceDefinition> generateDefinitions(TransferProcess process) {
        var dataRequest = process.getDataRequest();
        if (process.getType() == CONSUMER) {
            return dataRequest.isManagedResources() ? generateConsumerDefinitions(process) : emptyList();
        } else {
            return generateProviderDefinitions(process);
        }
    }

    @NotNull
    private List<ResourceDefinition> generateConsumerDefinitions(TransferProcess process) {
        return consumerGenerators.stream()
                .map(generator -> generator.generate(process))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    @NotNull
    private List<ResourceDefinition> generateProviderDefinitions(TransferProcess process) {
        return providerGenerators.stream()
                .map(generator -> generator.generate(process))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }
}
