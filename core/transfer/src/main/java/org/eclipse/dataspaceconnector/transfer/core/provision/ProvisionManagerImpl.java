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
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.eclipse.dataspaceconnector.common.async.AsyncUtils.asyncAllOf;

public class ProvisionManagerImpl implements ProvisionManager {
    private final List<Provisioner<?, ?>> provisioners = new ArrayList<>();
    private final Monitor monitor;

    public ProvisionManagerImpl(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public <RD extends ResourceDefinition, PR extends ProvisionedResource> void register(Provisioner<RD, PR> provisioner) {
        provisioners.add(provisioner);
    }

    @Override
    public CompletableFuture<List<ProvisionResponse>> provision(TransferProcess process) {
        return process.getResourceManifest().getDefinitions().stream()
                .map(definition -> provision(definition).whenComplete(logOnError(definition)))
                .collect(asyncAllOf());
    }

    @Override
    public CompletableFuture<List<DeprovisionResponse>> deprovision(TransferProcess process) {
        return process.getProvisionedResourceSet().getResources().stream()
                .map(definition -> deprovision(definition).whenComplete(logOnError(definition)))
                .collect(asyncAllOf());
    }

    @NotNull
    private CompletableFuture<ProvisionResponse> provision(ResourceDefinition definition) {
        try {
            return provisioners.stream()
                    .filter(it -> it.canProvision(definition))
                    .findFirst()
                    .map(it -> (Provisioner<ResourceDefinition, ?>) it)
                    .orElseThrow(() -> new EdcException("Unknown provision type" + definition.getClass().getName()))
                    .provision(definition);
        } catch (Exception e) {
            return failedFuture(e);
        }
    }

    @NotNull
    private CompletableFuture<DeprovisionResponse> deprovision(ProvisionedResource definition) {
        try {
            return provisioners.stream()
                    .filter(it -> it.canDeprovision(definition))
                    .findFirst()
                    .map(it -> (Provisioner<?, ProvisionedResource>) it)
                    .orElseThrow(() -> new EdcException("Unknown provision type" + definition.getClass().getName()))
                    .deprovision(definition);
        } catch (Exception e) {
            return failedFuture(e);
        }
    }

    @NotNull
    private BiConsumer<ProvisionResponse, Throwable> logOnError(ResourceDefinition definition) {
        return (result, throwable) -> {
            if (throwable != null) {
                monitor.severe(format("Error provisioning resource %s for process %s: %s", definition.getId(), definition.getTransferProcessId(), throwable.getMessage()));
            }
        };
    }

    @NotNull
    private BiConsumer<DeprovisionResponse, Throwable> logOnError(ProvisionedResource resource) {
        return (result, throwable) -> {
            if (throwable != null) {
                monitor.severe(format("Error deprovisioning resource %s for process %s: %s", resource.getId(), resource.getTransferProcessId(), throwable.getMessage()));
            }
        };
    }
}


