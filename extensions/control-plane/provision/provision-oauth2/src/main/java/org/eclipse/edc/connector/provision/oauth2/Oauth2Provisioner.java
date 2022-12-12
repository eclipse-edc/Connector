/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.provision.oauth2;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.connector.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Require an OAuth2 token and stores it in the vault to make data-plane include it in the request
 */
class Oauth2Provisioner implements Provisioner<Oauth2ResourceDefinition, Oauth2ProvisionedResource> {

    private final EdcHttpClient httpClient;
    private final TypeManager typeManager;

    Oauth2Provisioner(EdcHttpClient httpClient, TypeManager typeManager) {
        this.httpClient = httpClient;
        this.typeManager = typeManager;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof Oauth2ResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof Oauth2ProvisionedResource;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(Oauth2ResourceDefinition resourceDefinition, Policy policy) {
        var requestBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", resourceDefinition.getClientId())
                .add("client_secret", resourceDefinition.getClientSecret())
                .build();

        var url = resourceDefinition.getTokenUrl();
        var request = new Request.Builder().url(url).header("Accept", "application/json").post(requestBody).build();
        try (var response = httpClient.execute(request)) {

            var stringBody = getStringBody(response);
            if (!response.isSuccessful()) {
                return completedFuture(StatusResult.failure(FATAL_ERROR, format("OAuth2 server %s responded %s - %s at the client_credentials request", url, response.code(), stringBody)));
            }

            var responseBody = typeManager.readValue(stringBody, Map.class);
            var accessToken = responseBody.get("access_token");

            var resourceName = resourceDefinition.getId() + "-oauth2";
            var address = HttpDataAddress.Builder.newInstance()
                    .copyFrom(resourceDefinition.getDataAddress())
                    .secretName(resourceName)
                    .build();

            var provisioned = Oauth2ProvisionedResource.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .resourceDefinitionId(resourceDefinition.getId())
                    .transferProcessId(resourceDefinition.getTransferProcessId())
                    .dataAddress(address)
                    .resourceName(resourceName)
                    .hasToken(true)
                    .build();
            var provisionResponse = ProvisionResponse.Builder.newInstance()
                    .resource(provisioned)
                    .secretToken(new Oauth2SecretToken("Bearer " + accessToken))
                    .build();
            return completedFuture(StatusResult.success(provisionResponse));
        } catch (IOException e) {
            return completedFuture(StatusResult.failure(FATAL_ERROR, format("Error communicating with OAuth2 server %s: %s", url, e.getMessage())));
        }
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(Oauth2ProvisionedResource provisionedResource, Policy policy) {
        var deprovisionedResource = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId(provisionedResource.getId())
                .build();
        return completedFuture(StatusResult.success(deprovisionedResource));
    }

    @NotNull
    private String getStringBody(Response response) throws IOException {
        var body = response.body();
        if (body != null) {
            return body.string();
        } else {
            return "";
        }
    }
}
