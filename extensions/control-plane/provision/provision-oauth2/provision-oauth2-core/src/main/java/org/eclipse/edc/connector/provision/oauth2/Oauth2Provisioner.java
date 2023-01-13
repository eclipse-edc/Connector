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

import org.eclipse.edc.connector.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Require an OAuth2 token and stores it in the vault to make data-plane include it in the request
 */
class Oauth2Provisioner implements Provisioner<Oauth2ResourceDefinition, Oauth2ProvisionedResource> {

    private final Oauth2Client client;
    private final Oauth2CredentialsRequestFactory requestFactory;

    Oauth2Provisioner(Oauth2Client client, Oauth2CredentialsRequestFactory requestFactory) {
        this.client = client;
        this.requestFactory = requestFactory;
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
        var request = requestFactory.create(resourceDefinition);
        if (request.failed()) {
            return completedFuture(StatusResult.failure(FATAL_ERROR, request.getFailureDetail()));
        }

        var token = client.requestToken(request.getContent());
        if (token.failed()) {
            return completedFuture(StatusResult.failure(FATAL_ERROR, token.getFailureDetail()));
        }

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
                .secretToken(new Oauth2SecretToken("Bearer " + token.getContent().getToken()))
                .build();
        return completedFuture(StatusResult.success(provisionResponse));
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(Oauth2ProvisionedResource provisionedResource, Policy policy) {
        var deprovisionedResource = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId(provisionedResource.getId())
                .build();
        return completedFuture(StatusResult.success(deprovisionedResource));
    }

}
