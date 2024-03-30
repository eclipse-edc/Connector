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

package org.eclipse.edc.connector.controlplane.transfer.provision;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.eclipse.edc.util.async.AsyncUtils.asyncAllOf;

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

    @WithSpan
    @Override
    public CompletableFuture<List<StatusResult<ProvisionResponse>>> provision(List<ResourceDefinition> definitions, Policy policy) {
        return definitions.stream()
                .map(definition -> provision(definition, policy).whenComplete(logOnError(definition)))
                .collect(asyncAllOf());
    }

    @WithSpan
    @Override
    public CompletableFuture<List<StatusResult<DeprovisionedResource>>> deprovision(List<ProvisionedResource> resources, Policy policy) {
        return resources.stream()
                .map(definition -> deprovision(definition, policy).whenComplete(logOnError(definition)))
                .collect(asyncAllOf());
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private CompletableFuture<StatusResult<ProvisionResponse>> provision(ResourceDefinition definition, Policy policy) {
        try {
            return provisioners.stream()
                    .filter(it -> it.canProvision(definition))
                    .findFirst()
                    .map(it -> (Provisioner<ResourceDefinition, ?>) it)
                    .orElseThrow(() -> new EdcException("Unknown provision type " + definition.getClass().getName()))
                    .provision(definition, policy);
        } catch (Exception e) {
            return failedFuture(e);
        }
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(ProvisionedResource definition, Policy policy) {
        try {
            return provisioners.stream()
                    .filter(it -> it.canDeprovision(definition))
                    .findFirst()
                    .map(it -> (Provisioner<?, ProvisionedResource>) it)
                    .orElseThrow(() -> new EdcException("Unknown provision type " + definition.getClass().getName()))
                    .deprovision(definition, policy);
        } catch (Exception e) {
            return failedFuture(e);
        }
    }

    @NotNull
    private BiConsumer<StatusResult<ProvisionResponse>, Throwable> logOnError(ResourceDefinition definition) {
        return (result, throwable) -> {
            if (throwable != null) {
                monitor.severe(format("Error provisioning resource %s for process %s: %s", definition.getId(), definition.getTransferProcessId(), throwable.getMessage()));
            }
        };
    }

    @NotNull
    private BiConsumer<StatusResult<DeprovisionedResource>, Throwable> logOnError(ProvisionedResource resource) {
        return (result, throwable) -> {
            if (throwable != null) {
                monitor.severe(format("Error deprovisioning resource %s for process %s: %s", resource.getId(), resource.getTransferProcessId(), throwable.getMessage()));
            }
        };
    }
}


