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

import dev.failsafe.RetryPolicy;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.iam.oauth2.Oauth2Configuration;
import org.eclipse.edc.iam.oauth2.spi.CredentialsRequestAdditionalParametersProvider;
import org.eclipse.edc.jwt.spi.JwtDecorator;
import org.eclipse.edc.jwt.spi.JwtDecoratorRegistry;
import org.eclipse.edc.jwt.spi.TokenGenerationService;
import org.eclipse.edc.jwt.spi.TokenValidationService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

import static dev.failsafe.okhttp.FailsafeCall.with;
import static java.lang.String.format;

/**
 * Implements the OAuth2 client credentials flow and bearer token validation.
 */
public class Oauth2ServiceImpl implements IdentityService {

    private static final String GRANT_TYPE = "client_credentials";
    private static final String ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE = "Content-Type";

    private final Monitor monitor;
    private final Oauth2Configuration configuration;
    private final OkHttpClient httpClient;
    private final RetryPolicy<Response> retryPolicy;
    private final TypeManager typeManager;
    private final JwtDecoratorRegistry jwtDecoratorRegistry;
    private final TokenGenerationService tokenGenerationService;
    private final TokenValidationService tokenValidationService;
    private final CredentialsRequestAdditionalParametersProvider credentialsRequestAdditionalParametersProvider;

    /**
     * Creates a new instance of the OAuth2 Service
     *
     * @param monitor                                        The monitor
     * @param configuration                                  The configuration
     * @param tokenGenerationService                         Service used to generate the signed tokens;
     * @param client                                         Http client
     * @param retryPolicy                                    Retry policy
     * @param jwtDecoratorRegistry                           Registry containing the decorator for build the JWT
     * @param typeManager                                    Type manager
     * @param tokenValidationService                         Service used for token validation
     * @param credentialsRequestAdditionalParametersProvider Provides additional form parameters
     */
    public Oauth2ServiceImpl(Monitor monitor, Oauth2Configuration configuration, TokenGenerationService tokenGenerationService,
                             OkHttpClient client, RetryPolicy<Response> retryPolicy, JwtDecoratorRegistry jwtDecoratorRegistry,
                             TypeManager typeManager, TokenValidationService tokenValidationService,
                             CredentialsRequestAdditionalParametersProvider credentialsRequestAdditionalParametersProvider) {
        this.monitor = monitor;
        this.configuration = configuration;
        this.retryPolicy = retryPolicy;
        this.typeManager = typeManager;
        httpClient = client;
        this.jwtDecoratorRegistry = jwtDecoratorRegistry;
        this.tokenGenerationService = tokenGenerationService;
        this.tokenValidationService = tokenValidationService;
        this.credentialsRequestAdditionalParametersProvider = credentialsRequestAdditionalParametersProvider;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        return generateClientAssertion()
                .map(assertion -> createRequestBody(parameters, assertion))
                .map(this::createRequest)
                .compose(this::requestToken);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, String audience) {
        return tokenValidationService.validate(tokenRepresentation);
    }

    @NotNull
    private Result<String> generateClientAssertion() {
        var decorators = jwtDecoratorRegistry.getAll().toArray(JwtDecorator[]::new);
        return tokenGenerationService.generate(decorators)
                .map(TokenRepresentation::getToken);
    }

    @NotNull
    private FormBody createRequestBody(TokenParameters parameters, String assertion) {
        var requestBodyBuilder = new FormBody.Builder()
                .add("client_assertion_type", ASSERTION_TYPE)
                .add("grant_type", GRANT_TYPE)
                .add("client_assertion", assertion)
                .add("scope", parameters.getScope());

        credentialsRequestAdditionalParametersProvider.provide(parameters).forEach(requestBodyBuilder::add);

        return requestBodyBuilder.build();
    }

    @NotNull
    private Request createRequest(@NotNull FormBody requestBody) {
        return new Request.Builder()
                .url(configuration.getTokenUrl())
                .addHeader(CONTENT_TYPE, FORM_URLENCODED)
                .post(requestBody)
                .build();
    }

    @NotNull
    private Result<TokenRepresentation> requestToken(Request request) {
        try (
                var response = with(retryPolicy).compose(httpClient.newCall(request)).execute();
                var body = response.body()
        ) {
            if (body == null) {
                return failure("Response body is null");
            }

            if (!response.isSuccessful()) {
                return failure(format("Server responded with %s: %s", response.code(), body.string()));
            }

            var responseBody = body.string();

            try {
                var deserialized = typeManager.readValue(responseBody, Map.class);
                var token = (String) deserialized.get("access_token");
                var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(token).build();
                return Result.success(tokenRepresentation);
            } catch (Exception e) {
                return failure(format("Response body is not a valid json %s", responseBody), e);
            }
        } catch (IOException e) {
            return failure(e.getMessage(), e);
        }
    }

    @NotNull
    private Result<TokenRepresentation> failure(String message) {
        return Result.failure(failureMessage(message));
    }

    @NotNull
    private Result<TokenRepresentation> failure(String message, Exception e) {
        var fullMessage = failureMessage(message);
        monitor.severe(fullMessage, e);
        return Result.failure(fullMessage);
    }

    @NotNull
    private String failureMessage(String message) {
        return format("Error requesting oauth2 token from %s: %s", configuration.getTokenUrl(), message);
    }
}
