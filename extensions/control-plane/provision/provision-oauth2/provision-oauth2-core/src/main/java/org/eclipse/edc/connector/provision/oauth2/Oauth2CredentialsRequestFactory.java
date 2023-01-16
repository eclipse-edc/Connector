/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import org.eclipse.edc.iam.oauth2.spi.Oauth2AssertionDecorator;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.PrivateKeyOauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.jwt.TokenGenerationServiceImpl;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.time.Clock;
import java.util.Optional;

/**
 * Factory class that provides methods to build {@link Oauth2CredentialsRequest} instances
 */
public class Oauth2CredentialsRequestFactory {

    private static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";
    private final PrivateKeyResolver privateKeyResolver;
    private final Clock clock;
    private final Vault vault;
    private Monitor monitor;

    public Oauth2CredentialsRequestFactory(PrivateKeyResolver privateKeyResolver, Clock clock, Vault vault, Monitor monitor) {
        this.privateKeyResolver = privateKeyResolver;
        this.clock = clock;
        this.vault = vault;
        this.monitor = monitor;
    }

    /**
     * Create an {@link Oauth2CredentialsRequest} given a {@link Oauth2ResourceDefinition}
     *
     * @param resourceDefinition the resource definition
     * @return a {@link Result} containing the {@link Oauth2CredentialsRequest} object
     */
    public Result<Oauth2CredentialsRequest> create(Oauth2ResourceDefinition resourceDefinition) {
        var keySecret = resourceDefinition.getPrivateKeyName();
        return keySecret != null
                ? createPrivateKeyBasedRequest(keySecret, resourceDefinition)
                : createSharedSecretRequest(resourceDefinition);
    }

    @NotNull
    private Result<Oauth2CredentialsRequest> createPrivateKeyBasedRequest(String pkSecret, Oauth2ResourceDefinition resourceDefinition) {
        return createAssertion(pkSecret, resourceDefinition)
                .map(assertion -> PrivateKeyOauth2CredentialsRequest.Builder.newInstance()
                        .clientAssertion(assertion.getToken())
                        .url(resourceDefinition.getTokenUrl())
                        .grantType(GRANT_CLIENT_CREDENTIALS)
                        .scope(resourceDefinition.getScope())
                        .build());
    }

    @NotNull
    private Result<Oauth2CredentialsRequest> createSharedSecretRequest(Oauth2ResourceDefinition resourceDefinition) {
        var clientSecret = Optional.of(resourceDefinition)
                .map(Oauth2ResourceDefinition::getClientSecretKey)
                .map(vault::resolveSecret)
                .orElseGet(() -> {
                    monitor.warning("provision-oauth2: storing the client_secret into the DataAddress " +
                            "oauth2:clientSecret property has been deprecated, please store it in the Vault and use the " +
                            "oauth2:clientSecretKey property to reference it");
                    return resourceDefinition.getClientSecret();
                });

        if (clientSecret == null) {
            return Result.failure("Cannot resolve client secret from the vault: " + resourceDefinition.getClientSecretKey());
        }

        return Result.success(SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url(resourceDefinition.getTokenUrl())
                .grantType(GRANT_CLIENT_CREDENTIALS)
                .clientId(resourceDefinition.getClientId())
                .clientSecret(clientSecret)
                .scope(resourceDefinition.getScope())
                .build());
    }

    @NotNull
    private Result<TokenRepresentation> createAssertion(String pkSecret, Oauth2ResourceDefinition resourceDefinition) {
        var privateKey = privateKeyResolver.resolvePrivateKey(pkSecret, PrivateKey.class);
        if (privateKey == null) {
            return Result.failure("Failed to resolve private key with alias: " + pkSecret);
        }
        var decorator = new Oauth2AssertionDecorator(resourceDefinition.getTokenUrl(), resourceDefinition.getClientId(), clock, resourceDefinition.getValidity());
        var service = new TokenGenerationServiceImpl(privateKey);
        return service.generate(decorator);
    }
}
