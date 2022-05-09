/*
 *  Copyright (c) 2021, 2022 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *
 */

package com.siemens.mindsphere.provision;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.UUID.randomUUID;

public class FileSystemProvisioner
        implements Provisioner<FileSystemResourceDefinition, FileSystemProvisionedResource> {
    public FileSystemProvisioner(Monitor monitor, RetryPolicy<Object> retryPolicy) {
        this.monitor = monitor;
        this.retryPolicy = retryPolicy;
    }

    private final Monitor monitor;
    private final RetryPolicy<Object> retryPolicy;

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof FileSystemResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof FileSystemProvisionedResource;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(FileSystemResourceDefinition resourceDefinition,
                                                                        Policy policy) {
        return CompletableFuture.supplyAsync(() -> {
            final File file = new File(resourceDefinition.getPath());
            final File parentFolder = file.getParentFile() != null ? file.getParentFile() : new File(".");
            if (!parentFolder.exists()) {
                parentFolder.mkdirs();
            }

            try (var writer = new FileWriter(resourceDefinition.getPath(), true)) {
                final var response = ProvisionResponse
                        .Builder.newInstance()
                        .resource(FileSystemProvisionedResource.Builder.newInstance()
                                .id(randomUUID().toString())
                                .transferProcessId(resourceDefinition.getTransferProcessId())
                                .resourceDefinitionId(resourceDefinition.getId())
                                .resourceName(resourceDefinition.getPath())
                                .dataAddress(DataAddress.Builder.newInstance()
                                        .properties(
                                                Map.of("path", parentFolder.getAbsolutePath(),
                                                        "filename", file.getName()))
                                        .type("file").build())
                                .path(resourceDefinition.getPath())
                                .build())
                        .build();
                writer.write("Generated line at " + System.currentTimeMillis());
                return StatusResult.success(response);
            } catch (Exception e) {
                monitor.severe("Failed to provision " + resourceDefinition.getPath(), e);
                return StatusResult.failure(ResponseStatus.FATAL_ERROR);
            }
        });
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(FileSystemProvisionedResource provisionedResource,
                                                                              Policy policy) {
        return CompletableFuture.completedFuture(
                StatusResult.success(
                        DeprovisionedResource
                                .Builder.newInstance()
                                .provisionedResourceId(provisionedResource.getId())
                                .build()));
    }
}
