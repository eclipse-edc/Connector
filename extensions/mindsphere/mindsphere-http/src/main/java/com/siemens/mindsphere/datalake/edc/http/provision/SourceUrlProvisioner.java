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

package com.siemens.mindsphere.datalake.edc.http.provision;

import com.siemens.mindsphere.datalake.edc.http.DataLakeClientImpl;
import com.siemens.mindsphere.datalake.edc.http.DataLakeException;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.UUID.randomUUID;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.NAME;

/**
 * Generates HttpData schema with the download presign URL created
 */
public class SourceUrlProvisioner
        implements Provisioner<SourceUrlResourceDefinition, SourceUrlProvisionedResource> {

    private final DataLakeClientImpl clientImpl;

    public SourceUrlProvisioner(final DataLakeClientImpl clientImpl, final ServiceExtensionContext context, final RetryPolicy<Object> retryPolicy) {
        this.monitor = context.getMonitor();
        this.context = context;
        this.retryPolicy = retryPolicy;
        this.clientImpl = clientImpl;
    }

    private final ServiceExtensionContext context;
    private final Monitor monitor;
    private final RetryPolicy<Object> retryPolicy;

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof SourceUrlResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof SourceUrlProvisionedResource;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(SourceUrlResourceDefinition resourceDefinition,
                                                                        Policy policy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final var response = ProvisionResponse
                        .Builder.newInstance()
                        .resource(SourceUrlProvisionedResource.Builder.newInstance()
                                .id(randomUUID().toString())
                                .transferProcessId(resourceDefinition.getTransferProcessId())
                                .resourceDefinitionId(resourceDefinition.getId())
                                .resourceName(resourceDefinition.getDatalakePath())
                                .dataAddress(DataAddress.Builder.newInstance()
                                        .properties(
                                                Map.of(
                                                        ENDPOINT, createPresignedUrl(resourceDefinition.getDatalakePath()),
                                                        NAME, "",
                                                        MindsphereSchema.DATALAKE_PATH, resourceDefinition.getDatalakePath()))
                                        .type(HttpDataAddressSchema.TYPE).build())
                                .path(resourceDefinition.getDatalakePath())
                                .build())
                        .build();
                return StatusResult.success(response);
            } catch (Exception e) {
                monitor.severe("Failed to provision " + resourceDefinition.getDatalakePath(), e);
                return StatusResult.failure(ResponseStatus.FATAL_ERROR);
            }
        });
    }

    private String createPresignedUrl(final String datalakePath) {
        try {
            final URL createdUrl = clientImpl.getPresignedDownloadUrl(datalakePath);

            monitor.debug("Created presigned url: " + createdUrl.toString());
            return createdUrl.toString();
        } catch (DataLakeException e) {
            monitor.severe("Failed to generate presigned url for " + datalakePath, e);
            throw new IllegalArgumentException("Bad destination url given", e);
        }
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(SourceUrlProvisionedResource provisionedResource,
                                                                              Policy policy) {
        return CompletableFuture.completedFuture(
                StatusResult.success(
                        DeprovisionedResource
                                .Builder.newInstance()
                                .provisionedResourceId(provisionedResource.getId())
                                .build()));
    }
}
