/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.iam.oauth2.identity;

import org.eclipse.edc.iam.oauth2.Oauth2ServiceConfiguration;
import org.eclipse.edc.iam.oauth2.spi.CredentialsRequestAdditionalParametersProvider;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.PrivateKeyOauth2CredentialsRequest;
import org.eclipse.edc.jwt.spi.JwtDecorator;
import org.eclipse.edc.jwt.spi.JwtDecoratorRegistry;
import org.eclipse.edc.jwt.spi.TokenGenerationService;
import org.eclipse.edc.jwt.spi.TokenValidationService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.util.function.Supplier;

/**
 * Implements the OAuth2 client credentials flow and bearer token validation.
 */
public class Oauth2ServiceImpl implements IdentityService {

    private static final String GRANT_TYPE = "client_credentials";

    private final Oauth2ServiceConfiguration configuration;
    private final Supplier<PrivateKey> privateKeySupplier;
    private final Oauth2Client client;
    private final JwtDecoratorRegistry jwtDecoratorRegistry;
    private final TokenGenerationService tokenGenerationService;
    private final TokenValidationService tokenValidationService;
    private final CredentialsRequestAdditionalParametersProvider credentialsRequestAdditionalParametersProvider;

    /**
     * Creates a new instance of the OAuth2 Service
     *
     * @param configuration                                  The configuration
     * @param tokenGenerationService                         Service used to generate the signed tokens
     * @param client                                         client for Oauth2 server
     * @param jwtDecoratorRegistry                           Registry containing the decorator for build the JWT
     * @param tokenValidationService                         Service used for token validation
     * @param credentialsRequestAdditionalParametersProvider Provides additional form parameters
     */
    public Oauth2ServiceImpl(Oauth2ServiceConfiguration configuration, TokenGenerationService tokenGenerationService, Supplier<PrivateKey> privateKeySupplier,
                             Oauth2Client client, JwtDecoratorRegistry jwtDecoratorRegistry, TokenValidationService tokenValidationService,
                             CredentialsRequestAdditionalParametersProvider credentialsRequestAdditionalParametersProvider) {
        this.configuration = configuration;
        this.privateKeySupplier = privateKeySupplier;
        this.client = client;
        this.jwtDecoratorRegistry = jwtDecoratorRegistry;
        this.tokenGenerationService = tokenGenerationService;
        this.tokenValidationService = tokenValidationService;
        this.credentialsRequestAdditionalParametersProvider = credentialsRequestAdditionalParametersProvider;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        return generateClientAssertion()
                .map(assertion -> createRequest(parameters, assertion))
                .compose(client::requestToken);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, VerificationContext context) {
        return tokenValidationService.validate(tokenRepresentation);
    }

    @NotNull
    private Result<String> generateClientAssertion() {
        var decorators = jwtDecoratorRegistry.getAll().toArray(JwtDecorator[]::new);
        return tokenGenerationService.generate(privateKeySupplier, decorators)
                .map(TokenRepresentation::getToken);
    }

    @NotNull
    private Oauth2CredentialsRequest createRequest(TokenParameters parameters, String assertion) {
        return PrivateKeyOauth2CredentialsRequest.Builder.newInstance()
                .url(configuration.getTokenUrl())
                .clientAssertion(assertion)
                .scope(parameters.getScope())
                .grantType(GRANT_TYPE)
                .params(credentialsRequestAdditionalParametersProvider.provide(parameters))
                .build();
    }

}
