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

package org.eclipse.dataspaceconnector.transfer.provision.http.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.provision.DeprovisionResult;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionResult;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.transfer.provision.http.config.ProvisionerConfiguration;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.dataspaceconnector.transfer.provision.http.impl.HttpProvisionerRequest.Type.DEPROVISION;
import static org.eclipse.dataspaceconnector.transfer.provision.http.impl.HttpProvisionerRequest.Type.PROVISION;

/**
 * Invokes an HTTP endpoint to provision asset data. The endpoint will asynchronously return a content data address to a callback supplied with the address that can be used to
 * resolve the provisioned data.
 */
public class HttpProviderProvisioner implements Provisioner<HttpProviderResourceDefinition, HttpProvisionedContentResource> {
    private static final MediaType JSON = MediaType.get("application/json");

    private final String name;

    private final String dataAddressType;
    private final String policyScope;
    private final URL endpoint;
    private final URL callbackAddress;
    private final PolicyEngine policyEngine;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private final Monitor monitor;

    public HttpProviderProvisioner(ProvisionerConfiguration configuration,
                                   URL callbackAddress,
                                   PolicyEngine policyEngine,
                                   OkHttpClient httpClient,
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
        return resourceDefinition instanceof HttpProviderResourceDefinition && dataAddressType.equals(((HttpProviderResourceDefinition) resourceDefinition).getDataAddressType());
    }

    @Override
    public boolean canDeprovision(ProvisionedResource provisionedResource) {
        return provisionedResource instanceof HttpProvisionedContentResource &&
                dataAddressType.equals(((HttpProvisionedContentResource) provisionedResource).getDataAddress().getType());
    }

    @Override
    public CompletableFuture<ProvisionResult> provision(HttpProviderResourceDefinition resourceDefinition, Policy policy) {
        var scopedPolicy = policyEngine.filter(policy, policyScope);

        Request request;
        try {
            request = createRequest(PROVISION, resourceDefinition.getTransferProcessId(), resourceDefinition.getAssetId(), scopedPolicy);
        } catch (JsonProcessingException e) {
            monitor.severe("Error serializing provision request for provisioner: " + name, e);
            return completedFuture(ProvisionResult.failure(ResponseStatus.FATAL_ERROR, "Fatal error serializing request: " + e.getMessage()));
        }

        try (var response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return completedFuture(ProvisionResult.success(new ProvisionResponse()));   // in-process
            } else if (response.code() >= 500 && response.code() <= 504) {
                // retry
                return completedFuture(ProvisionResult.failure(ResponseStatus.ERROR_RETRY, "Received error code: " + response.code()));
            } else {
                // fatal error
                return completedFuture(ProvisionResult.failure(ResponseStatus.FATAL_ERROR, "Received fatal error code: " + response.code()));
            }
        } catch (IOException e) {
            monitor.severe("Error invoking provisioner: " + name, e);
            return completedFuture(ProvisionResult.failure(ResponseStatus.ERROR_RETRY, "Received error: " + e.getMessage()));
        }
    }

    @Override
    public CompletableFuture<DeprovisionResult> deprovision(HttpProvisionedContentResource provisionedResource, Policy policy) {
        // TODO expose scope from PolicyEngine
        var scopedPolicy = policy;

        Request request;
        try {
            request = createRequest(DEPROVISION, provisionedResource.getTransferProcessId(), provisionedResource.getAssetId(), scopedPolicy);
        } catch (JsonProcessingException e) {
            monitor.severe("Error serializing deprovision request for provisioner: " + name, e);
            return completedFuture(DeprovisionResult.failure(ResponseStatus.FATAL_ERROR, "Fatal error serializing request: " + e.getMessage()));
        }

        try (var response = httpClient.newCall(request).execute()) {
            if (response.code() == 200) {
                var deprovisionedResource = DeprovisionedResource.Builder.newInstance()
                        .provisionedResourceId(provisionedResource.getTransferProcessId())
                        .inProcess(true)
                        .build();
                return completedFuture(DeprovisionResult.success(deprovisionedResource));
            } else if (response.code() >= 500 && response.code() <= 504) {
                // retry
                return completedFuture(DeprovisionResult.failure(ResponseStatus.ERROR_RETRY, "Received error code: " + response.code()));
            } else {
                // fatal error
                return completedFuture(DeprovisionResult.failure(ResponseStatus.FATAL_ERROR, "Received fatal error code: " + response.code()));
            }
        } catch (IOException e) {
            monitor.severe("Error invoking provisioner: " + name, e);
            return completedFuture(DeprovisionResult.failure(ResponseStatus.ERROR_RETRY, "Received error: " + e.getMessage()));
        }

    }

    private Request createRequest(HttpProvisionerRequest.Type type, String processId, String assetId, Policy scopedPolicy) throws JsonProcessingException {
        var provisionerRequest = HttpProvisionerRequest.Builder.newInstance()
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
