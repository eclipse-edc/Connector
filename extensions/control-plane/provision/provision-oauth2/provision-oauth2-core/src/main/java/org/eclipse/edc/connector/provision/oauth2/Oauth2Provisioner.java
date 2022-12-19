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
import org.eclipse.edc.iam.oauth2.spi.Oauth2AssertionDecorator;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.PrivateKeyOauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.jwt.TokenGenerationServiceImpl;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Require an OAuth2 token and stores it in the vault to make data-plane include it in the request
 */
class Oauth2Provisioner implements Provisioner<Oauth2ResourceDefinition, Oauth2ProvisionedResource> {

    private static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";

    private final Oauth2Client client;
    private final PrivateKeyResolver privateKeyResolver;
    private final Clock clock;

    Oauth2Provisioner(Oauth2Client client, PrivateKeyResolver privateKeyResolver, Clock clock) {
        this.client = client;
        this.privateKeyResolver = privateKeyResolver;
        this.clock = clock;
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
        var request = createRequest(resourceDefinition);
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

    @NotNull
    private Result<Oauth2CredentialsRequest> createRequest(Oauth2ResourceDefinition rd) {
        var keySecret = rd.getPrivateKeyName();
        return keySecret != null ? createPrivateKeyBasedRequest(keySecret, rd) : createSharedSecretRequest(rd);
    }

    @NotNull
    private Result<Oauth2CredentialsRequest> createPrivateKeyBasedRequest(String pkSecret, Oauth2ResourceDefinition rd) {
        return createAssertion(pkSecret, rd)
                .map(assertion -> PrivateKeyOauth2CredentialsRequest.Builder.newInstance()
                        .clientAssertion(assertion.getToken())
                        .url(rd.getTokenUrl())
                        .grantType(GRANT_CLIENT_CREDENTIALS)
                        .build());
    }

    @NotNull
    private Result<Oauth2CredentialsRequest> createSharedSecretRequest(Oauth2ResourceDefinition rd) {
        return Result.success(SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url(rd.getTokenUrl())
                .grantType(GRANT_CLIENT_CREDENTIALS)
                .clientId(rd.getClientId())
                .clientSecret(rd.getClientSecret())
                .build());
    }

    private Result<TokenRepresentation> createAssertion(String pkSecret, Oauth2ResourceDefinition rd) {
        var privateKey = privateKeyResolver.resolvePrivateKey(pkSecret, PrivateKey.class);
        if (privateKey == null) {
            return Result.failure("Failed to resolve private key with alias: " + pkSecret);
        }
        var decorator = new Oauth2AssertionDecorator(rd.getTokenUrl(), rd.getClientId(), clock, rd.getValidity());
        var service = new TokenGenerationServiceImpl(privateKey);
        return service.generate(decorator);
    }
}
