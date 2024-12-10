/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.controlplane.provision.http.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.controlplane.provision.http.config.ProvisionerConfiguration;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Invokes an HTTP endpoint to provision asset data. The endpoint will asynchronously return a content data address to a
 * callback supplied with the address that can be used to resolve the provisioned data.
 */
public class HttpProviderProvisioner implements Provisioner<HttpProviderResourceDefinition, HttpProvisionedContentResource> {
    private static final MediaType JSON = MediaType.get("application/json");

    private final String name;

    private final String dataAddressType;
    private final String policyScope;
    private final URL endpoint;
    private final URL callbackAddress;
    private final PolicyEngine policyEngine;
    private final EdcHttpClient httpClient;
    private final ObjectMapper mapper;
    private final Monitor monitor;

    public HttpProviderProvisioner(ProvisionerConfiguration configuration,
                                   URL callbackAddress,
                                   PolicyEngine policyEngine,
                                   EdcHttpClient httpClient,
                                   ObjectMapper objectMapper,
                                   Monitor monitor) {
        name = configuration.getName();
        dataAddressType = configuration.getDataAddressType();
        policyScope = configuration.getPolicyScope();
        endpoint = configuration.getEndpoint();
        this.callbackAddress = callbackAddress;
        this.policyEngine = policyEngine;
        this.httpClient = httpClient;
        mapper = objectMapper;
        this.monitor = monitor;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof HttpProviderResourceDefinition definition && dataAddressType.equals(definition.getDataAddressType());
    }

    @Override
    public boolean canDeprovision(ProvisionedResource provisionedResource) {
        return provisionedResource instanceof HttpProvisionedContentResource;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(HttpProviderResourceDefinition resourceDefinition, Policy policy) {
        var scopedPolicy = policyEngine.filter(policy, policyScope);

        Request request;
        try {
            request = createRequest(HttpProvisionerRequest.Type.PROVISION, resourceDefinition.getId(), resourceDefinition.getTransferProcessId(), resourceDefinition.getAssetId(), scopedPolicy);
        } catch (JsonProcessingException e) {
            monitor.severe("Error serializing provision request for provisioner: " + name, e);
            return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, "HttpProviderProvisioner: fatal error serializing request: " + e.getMessage()));
        }

        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                return completedFuture(StatusResult.success(ProvisionResponse.Builder.newInstance().inProcess(true).build()));
            } else if (response.code() >= 500 && response.code() <= 504) {
                return completedFuture(StatusResult.failure(ResponseStatus.ERROR_RETRY, "HttpProviderProvisioner: received error code: " + response.code()));
            } else {
                return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, "HttpProviderProvisioner: received fatal error code: " + response.code()));
            }
        } catch (IOException e) {
            monitor.severe("Error invoking provisioner: " + name, e);
            return completedFuture(StatusResult.failure(ResponseStatus.ERROR_RETRY, "HttpProviderProvisioner: received error: " + e.getMessage()));
        }
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(HttpProvisionedContentResource provisionedResource, Policy policy) {
        // TODO expose scope from PolicyEngine
        var scopedPolicy = policy;

        Request request;
        try {
            request = createRequest(HttpProvisionerRequest.Type.DEPROVISION, provisionedResource.getResourceDefinitionId(), provisionedResource.getTransferProcessId(), provisionedResource.getAssetId(), scopedPolicy);
        } catch (JsonProcessingException e) {
            monitor.severe("Error serializing deprovision request for provisioner: " + name, e);
            return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, "Fatal error serializing request: " + e.getMessage()));
        }

        try (var response = httpClient.execute(request)) {
            if (response.code() == 200) {
                var deprovisionedResource = DeprovisionedResource.Builder.newInstance()
                        .provisionedResourceId(provisionedResource.getId())
                        .inProcess(true)
                        .build();
                return completedFuture(StatusResult.success(deprovisionedResource));
            } else if (response.code() >= 500 && response.code() <= 504) {
                return completedFuture(StatusResult.failure(ResponseStatus.ERROR_RETRY, "Received error code: " + response.code()));
            } else {
                return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, "Received fatal error code: " + response.code()));
            }
        } catch (IOException e) {
            monitor.severe("Error invoking provisioner: " + name, e);
            return completedFuture(StatusResult.failure(ResponseStatus.ERROR_RETRY, "Received error: " + e.getMessage()));
        }

    }

    private Request createRequest(HttpProvisionerRequest.Type type, String resourceDefinitionId, String processId, String assetId, Policy scopedPolicy) throws JsonProcessingException {
        var provisionerRequest = HttpProvisionerRequest.Builder.newInstance()
                .resourceDefinitionId(resourceDefinitionId)
                .assetId(assetId)
                .transferProcessId(processId)
                .type(type)
                .policy(scopedPolicy)
                .callbackAddress(callbackAddress.toString())
                .build();
        var requestBody = RequestBody.create(mapper.writeValueAsString(provisionerRequest), JSON);
        return new Request.Builder().url(endpoint).post(requestBody).build();
    }

}
