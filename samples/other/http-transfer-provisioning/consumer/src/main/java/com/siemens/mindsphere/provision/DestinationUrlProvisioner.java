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

import com.siemens.mindsphere.datalake.edc.http.DataLakeClientImpl;
import com.siemens.mindsphere.datalake.edc.http.DataLakeException;
import com.siemens.mindsphere.datalake.edc.http.OauthClientDetails;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.UUID.randomUUID;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.NAME;

public class DestinationUrlProvisioner
        implements Provisioner<DestinationUrlResourceDefinition, DestinationUrlProvisionedResource> {


    @EdcSetting
    private static final String TOKENMANAGEMENT_ADDRESS = "datalake.oauth.address";

    @EdcSetting
    private static final String TOKENMANAGEMENT_CLIENT_ID = "datalake.oauth.client.id";

    @EdcSetting
    private static final String TOKENMANAGEMENT_CLIENT_SECRET = "datalake.oauth.client.secret";

    @EdcSetting
    private static final String TOKENMANAGEMENT_CLIENT_APP_NAME = "datalake.oauth.client.app.name";

    @EdcSetting
    private static final String TOKENMANAGEMENT_CLIENT_APP_VERSION = "datalake.oauth.client.app.version";

    @EdcSetting
    private static final String APPLICATION_TENANT = "datalake.tenant";

    @EdcSetting
    private static final String DATALAKE_DOWNLOAD_ADDRESS = "datalake.generatedownloadbjecturls.endpoint";

    public DestinationUrlProvisioner(ServiceExtensionContext context, RetryPolicy<Object> retryPolicy) {
        this.monitor = context.getMonitor();
        this.context = context;
        this.retryPolicy = retryPolicy;
    }

    private final ServiceExtensionContext context;
    private final Monitor monitor;
    private final RetryPolicy<Object> retryPolicy;

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof DestinationUrlResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof DestinationUrlProvisionedResource;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(DestinationUrlResourceDefinition resourceDefinition,
                                                                        Policy policy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final var response = ProvisionResponse
                        .Builder.newInstance()
                        .resource(DestinationUrlProvisionedResource.Builder.newInstance()
                                .id(randomUUID().toString())
                                .transferProcessId(resourceDefinition.getTransferProcessId())
                                .resourceDefinitionId(resourceDefinition.getId())
                                .resourceName(resourceDefinition.getDatalakePath())
                                .dataAddress(DataAddress.Builder.newInstance()
                                        .properties(
                                                Map.of(
                                                        ENDPOINT, createPresignedUrl(resourceDefinition.getDatalakePath()),
                                                        NAME, "",
                                                        "HttpData", resourceDefinition.getDatalakePath()))
                                        .type(HttpDataAddressSchema.TYPE).build())
                                .url(resourceDefinition.getDatalakePath())
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
        final String tokenmanagementClientId = context.getSetting(TOKENMANAGEMENT_CLIENT_ID, "");
        final String tokenmanagementClientSecret = context.getSetting(TOKENMANAGEMENT_CLIENT_SECRET, "");
        final String tokenmanagementClientAppName = context.getSetting(TOKENMANAGEMENT_CLIENT_APP_NAME, "");
        final String tokenmanagementClientAppVersion = context.getSetting(TOKENMANAGEMENT_CLIENT_APP_VERSION, "");
        final String tokenmanagementAddress = context.getSetting(TOKENMANAGEMENT_ADDRESS, "");
        final String dataLakeAddress = context.getSetting(DATALAKE_DOWNLOAD_ADDRESS, "");

        final String applicationTenant = context.getSetting(APPLICATION_TENANT, "presdev");

        monitor.debug("datalake.oauth.clientId=" + tokenmanagementClientId);
        monitor.debug("datalake.oauth.clientSecret=" + tokenmanagementClientSecret);
        monitor.debug("datalake.tenant=" + applicationTenant);
        monitor.debug("datalake.address=" + dataLakeAddress);

        final URL url;
        try {
            url = new URL(dataLakeAddress);

            final OauthClientDetails oauthClientDetails = new OauthClientDetails(tokenmanagementClientId, tokenmanagementClientSecret,
                    tokenmanagementClientAppName, tokenmanagementClientAppVersion, applicationTenant, new URL(tokenmanagementAddress));

            final DataLakeClientImpl clientImpl = new DataLakeClientImpl(oauthClientDetails, url);

            final URL createdUrl = clientImpl.getPresignedUploadUrl(datalakePath);

            monitor.debug("Created presigned url: " + createdUrl.toString());
            return createdUrl.toString();
        } catch (MalformedURLException | DataLakeException e) {
            monitor.severe("Failed to generate presigned url for " + datalakePath, e);
            throw new IllegalArgumentException("Bad destination url given", e);
        }
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(DestinationUrlProvisionedResource provisionedResource,
                                                                              Policy policy) {
        return CompletableFuture.completedFuture(
                StatusResult.success(
                        DeprovisionedResource
                                .Builder.newInstance()
                                .provisionedResourceId(provisionedResource.getId())
                                .build()));
    }
}
