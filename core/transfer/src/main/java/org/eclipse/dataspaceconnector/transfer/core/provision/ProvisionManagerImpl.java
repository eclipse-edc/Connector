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
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default provision manager.
 */
public class ProvisionManagerImpl implements ProvisionManager {
    private final List<Provisioner<?, ?>> provisioners = new ArrayList<>();

    public ProvisionManagerImpl() {
    }

    public void start(ProvisionContext provisionContext) {
        provisioners.forEach(provisioner -> provisioner.initialize(provisionContext));
    }

    @Override
    public <RD extends ResourceDefinition, PR extends ProvisionedResource> void register(Provisioner<RD, PR> provisioner) {
        provisioners.add(provisioner);
    }

    @Override
    public void provision(TransferProcess process) {
        for (ResourceDefinition definition : process.getResourceManifest().getDefinitions()) {
            Provisioner<ResourceDefinition, ?> chosenProvisioner = getProvisioner(definition);
            var status = chosenProvisioner.provision(definition);
        }
    }

    @Override
    public List<ResponseStatus> deprovision(TransferProcess process) {
        return process.getProvisionedResourceSet().getResources().stream()
                .map(definition -> {
                    Provisioner<?, ProvisionedResource> chosenProvisioner = getProvisioner(definition);
                    return chosenProvisioner.deprovision(definition);
                })
                .collect(Collectors.toList());
    }

    @NotNull
    private Provisioner<ResourceDefinition, ?> getProvisioner(ResourceDefinition definition) {
        Provisioner<ResourceDefinition, ?> provisioner = null;
        for (Provisioner<?, ?> candidate : provisioners) {
            if (candidate.canProvision(definition)) {
                provisioner = (Provisioner<ResourceDefinition, ?>) candidate;
                break;
            }
        }
        if (provisioner == null) {
            throw new EdcException("Unknown provision type" + definition.getClass().getName());
        }
        return provisioner;
    }

    @NotNull
    private Provisioner<?, ProvisionedResource> getProvisioner(ProvisionedResource provisionedResource) {
        Provisioner<?, ProvisionedResource> provisioner = null;
        for (Provisioner<?, ?> candidate : provisioners) {
            if (candidate.canDeprovision(provisionedResource)) {
                provisioner = (Provisioner<?, ProvisionedResource>) candidate;
                break;
            }
        }
        if (provisioner == null) {
            throw new EdcException("Unknown provision type" + provisionedResource.getClass().getName());
        }
        return provisioner;
    }


}
