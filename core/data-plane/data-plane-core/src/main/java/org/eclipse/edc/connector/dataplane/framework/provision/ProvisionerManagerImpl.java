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

import org.eclipse.edc.connector.dataplane.spi.provision.DeprovisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.Deprovisioner;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.Provisioner;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.eclipse.edc.util.async.AsyncUtils.asyncAllOf;

public class ProvisionerManagerImpl implements ProvisionerManager {

    private final List<Provisioner> provisioners = new ArrayList<>();
    private final List<Deprovisioner> deprovisioners = new ArrayList<>();
    private final Monitor monitor;

    public ProvisionerManagerImpl(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void register(Provisioner provisioner) {
        provisioners.add(provisioner);
    }

    @Override
    public void register(Deprovisioner deprovisioner) {
        deprovisioners.add(deprovisioner);
    }

    @Override
    public CompletableFuture<List<StatusResult<ProvisionedResource>>> provision(List<ProvisionResource> definitions) {
        return definitions.stream()
                .map(definition -> provision(definition).whenComplete(logOnError(definition)))
                .collect(asyncAllOf());
    }

    @Override
    public CompletableFuture<List<StatusResult<DeprovisionedResource>>> deprovision(List<ProvisionResource> definitions) {
        return definitions.stream()
                .map(definition -> deprovision(definition).whenComplete(logOnError(definition)))
                .collect(asyncAllOf());
    }

    @NotNull
    private CompletableFuture<StatusResult<ProvisionedResource>> provision(ProvisionResource definition) {
        try {
            return provisioners.stream()
                    .filter(it -> it.supportedType().equals(definition.getType()))
                    .findFirst()
                    .map(p -> p.provision(definition))
                    .orElseGet(() -> failedFuture(new EdcException("No provisioner available for definition type %s".formatted(definition.getType()))));
        } catch (Exception e) {
            return failedFuture(e);
        }
    }

    @NotNull
    private CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(ProvisionResource definition) {
        try {
            return deprovisioners.stream()
                    .filter(it -> it.supportedType().equals(definition.getType()))
                    .findFirst()
                    .map(p -> p.deprovision(definition))
                    .orElseGet(() -> failedFuture(new EdcException("No deprovisioner available for definition type %s".formatted(definition.getType()))));
        } catch (Exception e) {
            return failedFuture(e);
        }
    }

    @NotNull
    private BiConsumer<StatusResult<?>, Throwable> logOnError(ProvisionResource definition) {
        return (result, throwable) -> {
            if (throwable != null) {
                monitor.severe("Error provisioning definition %s for flow %s: %s".formatted(definition.getId(), definition.getFlowId(), throwable.getMessage()));
            }
        };
    }
}
