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

package org.eclipse.edc.connector.dataplane.http.oauth2;

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
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.time.Clock;
import java.util.Optional;

import static org.eclipse.edc.connector.dataplane.http.oauth2.Oauth2DataAddressSchema.CLIENT_ID;
import static org.eclipse.edc.connector.dataplane.http.oauth2.Oauth2DataAddressSchema.CLIENT_SECRET;
import static org.eclipse.edc.connector.dataplane.http.oauth2.Oauth2DataAddressSchema.CLIENT_SECRET_KEY;
import static org.eclipse.edc.connector.dataplane.http.oauth2.Oauth2DataAddressSchema.PRIVATE_KEY_NAME;
import static org.eclipse.edc.connector.dataplane.http.oauth2.Oauth2DataAddressSchema.SCOPE;
import static org.eclipse.edc.connector.dataplane.http.oauth2.Oauth2DataAddressSchema.TOKEN_URL;
import static org.eclipse.edc.connector.dataplane.http.oauth2.Oauth2DataAddressSchema.VALIDITY;

/**
 * Factory class that provides methods to build {@link Oauth2CredentialsRequest} instances
 */
public class Oauth2CredentialsRequestFactory {

    private static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";
    private final PrivateKeyResolver privateKeyResolver;
    private final Clock clock;
    private final Vault vault;
    private final Monitor monitor;

    public Oauth2CredentialsRequestFactory(PrivateKeyResolver privateKeyResolver, Clock clock, Vault vault, Monitor monitor) {
        this.privateKeyResolver = privateKeyResolver;
        this.clock = clock;
        this.vault = vault;
        this.monitor = monitor;
    }

    /**
     * Create an {@link Oauth2CredentialsRequest} given a {@link org.eclipse.edc.spi.types.domain.DataAddress}
     *
     * @param dataAddress the data address
     * @return a {@link Result} containing the {@link Oauth2CredentialsRequest} object
     */
    public Result<Oauth2CredentialsRequest> create(DataAddress dataAddress) {
        var keySecret = dataAddress.getProperty(PRIVATE_KEY_NAME);
        return keySecret != null
                ? createPrivateKeyBasedRequest(keySecret, dataAddress)
                : createSharedSecretRequest(dataAddress);
    }

    @NotNull
    private Result<Oauth2CredentialsRequest> createPrivateKeyBasedRequest(String pkSecret, DataAddress dataAddress) {
        return createAssertion(pkSecret, dataAddress)
                .map(assertion -> PrivateKeyOauth2CredentialsRequest.Builder.newInstance()
                        .clientAssertion(assertion.getToken())
                        .url(dataAddress.getProperty(TOKEN_URL))
                        .grantType(GRANT_CLIENT_CREDENTIALS)
                        .scope(dataAddress.getProperty(SCOPE))
                        .build());
    }

    @NotNull
    private Result<Oauth2CredentialsRequest> createSharedSecretRequest(DataAddress dataAddress) {
        var clientSecret = Optional.of(dataAddress)
                .map(a -> a.getProperty(CLIENT_SECRET_KEY))
                .map(vault::resolveSecret)
                .orElseGet(() -> {
                    monitor.warning("provision-oauth2: storing the client_secret into the DataAddress " +
                            "oauth2:clientSecret property has been deprecated, please store it in the Vault and use the " +
                            "oauth2:clientSecretKey property to reference it");
                    return dataAddress.getProperty(CLIENT_SECRET);
                });

        if (clientSecret == null) {
            return Result.failure("Cannot resolve client secret from the vault: " + dataAddress.getProperty(CLIENT_SECRET_KEY));
        }

        return Result.success(SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url(dataAddress.getProperty(TOKEN_URL))
                .grantType(GRANT_CLIENT_CREDENTIALS)
                .clientId(dataAddress.getProperty(CLIENT_ID))
                .clientSecret(clientSecret)
                .scope(dataAddress.getProperty(SCOPE))
                .build());
    }

    @NotNull
    private Result<TokenRepresentation> createAssertion(String pkSecret, DataAddress dataAddress) {
        var privateKey = privateKeyResolver.resolvePrivateKey(pkSecret, PrivateKey.class);
        if (privateKey == null) {
            return Result.failure("Failed to resolve private key with alias: " + pkSecret);
        }
        var decorator = new Oauth2AssertionDecorator(dataAddress.getProperty(TOKEN_URL), dataAddress.getProperty(CLIENT_ID), clock, Long.parseLong(dataAddress.getProperty(VALIDITY)));
        var service = new TokenGenerationServiceImpl(privateKey);
        return service.generate(decorator);
    }
}
