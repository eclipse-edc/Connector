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
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.token.JwtGenerationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.CLIENT_ID;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.CLIENT_SECRET_KEY;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.KEY_ID;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.PRIVATE_KEY_NAME;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.SCOPE;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.TOKEN_URL;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.VALIDITY;

/**
 * Factory class that provides methods to build {@link Oauth2CredentialsRequest} instances
 */
public class Oauth2CredentialsRequestFactory {

    private static final long DEFAULT_TOKEN_VALIDITY = TimeUnit.MINUTES.toSeconds(5);
    private static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";
    private final JwsSignerProvider jwsSignerProvider;
    private final Clock clock;
    private final Vault vault;

    public Oauth2CredentialsRequestFactory(JwsSignerProvider jwsSignerProvider, Clock clock, Vault vault) {
        this.jwsSignerProvider = jwsSignerProvider;
        this.clock = clock;
        this.vault = vault;
    }

    /**
     * Create an {@link Oauth2CredentialsRequest} given a {@link org.eclipse.edc.spi.types.domain.DataAddress}
     *
     * @param dataAddress the data address
     * @return a {@link Result} containing the {@link Oauth2CredentialsRequest} object
     */
    public Result<Oauth2CredentialsRequest> create(DataAddress dataAddress) {
        var keySecret = dataAddress.getStringProperty(PRIVATE_KEY_NAME);
        return keySecret != null
                ? createPrivateKeyBasedRequest(keySecret, dataAddress)
                : createSharedSecretRequest(dataAddress);
    }

    @NotNull
    private Result<Oauth2CredentialsRequest> createPrivateKeyBasedRequest(String pkSecret, DataAddress dataAddress) {
        return createAssertion(pkSecret, dataAddress)
                .map(assertion -> PrivateKeyOauth2CredentialsRequest.Builder.newInstance()
                        .clientAssertion(assertion.getToken())
                        .url(dataAddress.getStringProperty(TOKEN_URL))
                        .grantType(GRANT_CLIENT_CREDENTIALS)
                        .scope(dataAddress.getStringProperty(SCOPE))
                        .build());
    }

    @NotNull
    private Result<Oauth2CredentialsRequest> createSharedSecretRequest(DataAddress dataAddress) {
        var clientSecret = Optional.of(dataAddress)
                .map(a -> a.getStringProperty(CLIENT_SECRET_KEY))
                .map(vault::resolveSecret)
                .orElse(null);

        if (clientSecret == null) {
            return Result.failure("Cannot resolve client secret from the vault: " + dataAddress.getStringProperty(CLIENT_SECRET_KEY));
        }

        return Result.success(SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url(dataAddress.getStringProperty(TOKEN_URL))
                .grantType(GRANT_CLIENT_CREDENTIALS)
                .clientId(dataAddress.getStringProperty(CLIENT_ID))
                .clientSecret(clientSecret)
                .scope(dataAddress.getStringProperty(SCOPE))
                .build());
    }

    @NotNull
    private Result<TokenRepresentation> createAssertion(String pkSecret, DataAddress dataAddress) {
        var validity = Optional.ofNullable(dataAddress.getStringProperty(VALIDITY))
                .map(this::parseLong)
                .orElse(DEFAULT_TOKEN_VALIDITY);
        var decorator = Oauth2AssertionDecorator.Builder.newInstance()
                .audience(dataAddress.getStringProperty(TOKEN_URL))
                .clientId(dataAddress.getStringProperty(CLIENT_ID))
                .clock(clock)
                .validity(validity)
                .kid(dataAddress.getStringProperty(KEY_ID))
                .build();
        var service = new JwtGenerationService(jwsSignerProvider);

        return service.generate(pkSecret, decorator);
    }

    @Nullable
    private Long parseLong(String v) {
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
