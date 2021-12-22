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

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.transfer.provision.DeprovisionResponse;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Default provision manager.
 */
public class ProvisionManagerImpl implements ProvisionManager {
    private final List<Provisioner<?, ?>> provisioners = new ArrayList<>();

    @Override
    public <RD extends ResourceDefinition, PR extends ProvisionedResource> void register(Provisioner<RD, PR> provisioner) {
        provisioners.add(provisioner);
    }

    @Override
    public List<CompletableFuture<ProvisionResponse>> provision(TransferProcess process) {
        return process.getResourceManifest().getDefinitions().stream()
                .map(definition -> {
                    try {
                        return getProvisioner(definition).provision(definition);
                    } catch (Exception e) {
                        return CompletableFuture.<ProvisionResponse>failedFuture(e);
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CompletableFuture<DeprovisionResponse>> deprovision(TransferProcess process) {
        return process.getProvisionedResourceSet().getResources().stream()
                .map(definition -> {
                    try {
                        return getProvisioner(definition).deprovision(definition);
                    } catch (Exception e) {
                        return CompletableFuture.<DeprovisionResponse>failedFuture(e);
                    }
                })
                .collect(Collectors.toList());
    }

    @NotNull
    private Provisioner<ResourceDefinition, ?> getProvisioner(ResourceDefinition definition) {
        return provisioners.stream()
                .filter(it -> it.canProvision(definition))
                .findFirst()
                .map(it -> (Provisioner<ResourceDefinition, ?>) it)
                .orElseThrow(() -> new EdcException("Unknown provision type" + definition.getClass().getName()));
    }

    @NotNull
    private Provisioner<?, ProvisionedResource> getProvisioner(ProvisionedResource provisionedResource) {
        return provisioners.stream()
                .filter(it -> it.canDeprovision(provisionedResource))
                .findFirst()
                .map(it -> (Provisioner<?, ProvisionedResource>) it)
                .orElseThrow(() -> new EdcException("Unknown provision type" + provisionedResource.getClass().getName()));
    }


}
