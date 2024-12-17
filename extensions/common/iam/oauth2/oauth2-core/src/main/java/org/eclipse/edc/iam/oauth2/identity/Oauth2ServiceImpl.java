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

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.PrivateKeyOauth2CredentialsRequest.Builder;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenDecoratorRegistry;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static org.eclipse.edc.iam.oauth2.Oauth2ServiceExtension.OAUTH2_TOKEN_CONTEXT;

/**
 * Implements the OAuth2 client credentials flow and bearer token validation.
 */
public class Oauth2ServiceImpl implements IdentityService {

    private static final String GRANT_TYPE = "client_credentials";

    private final String tokenUrl;
    private final Supplier<String> privateKeySupplier;
    private final Oauth2Client client;
    private final TokenDecoratorRegistry jwtDecoratorRegistry;
    private final TokenGenerationService tokenGenerationService;
    private final TokenValidationService tokenValidationService;
    private final PublicKeyResolver publicKeyResolver;
    private final TokenValidationRulesRegistry tokenValidationRuleRegistry;
    private final boolean tokenResourceEnabled;

    /**
     * Creates a new instance of the OAuth2 Service
     *
     * @param tokenUrl               Token URL
     * @param tokenGenerationService Service used to generate the signed tokens
     * @param client                 client for Oauth2 server
     * @param jwtDecoratorRegistry   Registry containing the decorator for build the JWT
     * @param tokenValidationService Service used for token validation
     * @param tokenResourceEnabled   Add support for generating access token request with resource parameter
     */
    public Oauth2ServiceImpl(String tokenUrl, TokenGenerationService tokenGenerationService, Supplier<String> privateKeyIdSupplier,
                             Oauth2Client client, TokenDecoratorRegistry jwtDecoratorRegistry, TokenValidationRulesRegistry tokenValidationRuleRegistry, TokenValidationService tokenValidationService,
                             PublicKeyResolver publicKeyResolver, boolean tokenResourceEnabled) {
        this.tokenUrl = tokenUrl;
        this.privateKeySupplier = privateKeyIdSupplier;
        this.client = client;
        this.jwtDecoratorRegistry = jwtDecoratorRegistry;
        this.tokenValidationRuleRegistry = tokenValidationRuleRegistry;
        this.tokenGenerationService = tokenGenerationService;
        this.tokenValidationService = tokenValidationService;
        this.publicKeyResolver = publicKeyResolver;
        this.tokenResourceEnabled = tokenResourceEnabled;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        return generateClientAssertion()
                .map(assertion -> createRequest(parameters, assertion))
                .compose(client::requestToken);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, VerificationContext context) {
        return tokenValidationService.validate(tokenRepresentation, publicKeyResolver, tokenValidationRuleRegistry.getRules(OAUTH2_TOKEN_CONTEXT));
    }

    @NotNull
    private Result<String> generateClientAssertion() {
        var decorators = jwtDecoratorRegistry.getDecoratorsFor(OAUTH2_TOKEN_CONTEXT).toArray(TokenDecorator[]::new);
        return tokenGenerationService.generate(privateKeySupplier.get(), decorators)
                .map(TokenRepresentation::getToken);
    }

    @NotNull
    private Oauth2CredentialsRequest createRequest(TokenParameters parameters, String assertion) {
        var builder = Builder.newInstance()
                .url(tokenUrl)
                .clientAssertion(assertion)
                .scope(parameters.getStringClaim(JwtRegisteredClaimNames.SCOPE))
                .grantType(GRANT_TYPE);

        if (tokenResourceEnabled) {
            builder.resource(parameters.getStringClaim(JwtRegisteredClaimNames.AUDIENCE));
        }
        return builder.build();
    }

}
