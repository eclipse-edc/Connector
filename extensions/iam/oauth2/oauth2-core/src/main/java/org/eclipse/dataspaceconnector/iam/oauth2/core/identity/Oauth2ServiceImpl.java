/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core.identity;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.common.token.JwtDecorator;
import org.eclipse.dataspaceconnector.common.token.JwtDecoratorRegistry;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationService;
import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.iam.oauth2.core.Oauth2Configuration;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * Implements the OAuth2 client credentials flow and bearer token validation.
 */
public class Oauth2ServiceImpl implements IdentityService {

    private static final String GRANT_TYPE = "client_credentials";
    private static final String ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final Oauth2Configuration configuration;
    private final OkHttpClient httpClient;
    private final TypeManager typeManager;
    private final JwtDecoratorRegistry jwtDecoratorRegistry;
    private final TokenGenerationService tokenGenerationService;
    private final TokenValidationService tokenValidationService;

    /**
     * Creates a new instance of the OAuth2 Service
     *
     * @param configuration          The configuration
     * @param tokenGenerationService Service used to generate the signed tokens;
     * @param client                 Http client
     * @param jwtDecoratorRegistry   Registry containing the decorator for build the JWT
     * @param typeManager            Type manager
     * @param tokenValidationService Service used for token validation
     */
    public Oauth2ServiceImpl(Oauth2Configuration configuration, TokenGenerationService tokenGenerationService, OkHttpClient client, JwtDecoratorRegistry jwtDecoratorRegistry, TypeManager typeManager,
                             TokenValidationService tokenValidationService) {
        this.configuration = configuration;
        this.typeManager = typeManager;
        httpClient = client;
        this.jwtDecoratorRegistry = jwtDecoratorRegistry;
        this.tokenGenerationService = tokenGenerationService;
        this.tokenValidationService = tokenValidationService;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(String scope) {
        var jwtCreationResult = tokenGenerationService.generate(jwtDecoratorRegistry.getAll().toArray(JwtDecorator[]::new));
        if (jwtCreationResult.failed()) {
            return jwtCreationResult;
        }

        var assertion = jwtCreationResult.getContent().getToken();
        var requestBody = new FormBody.Builder()
                .add("client_assertion_type", ASSERTION_TYPE)
                .add("grant_type", GRANT_TYPE)
                .add("client_assertion", assertion)
                .add("scope", scope)
                .build();

        var request = new Request.Builder().url(configuration.getTokenUrl()).addHeader("Content-Type", CONTENT_TYPE).post(requestBody).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                try (var body = response.body()) {
                    String message = body == null ? "<empty body>" : body.string();
                    return Result.failure(message);
                }
            }

            var responseBody = response.body();
            if (responseBody == null) {
                return Result.failure("<empty token body>");
            }

            var responsePayload = responseBody.string();
            var deserialized = typeManager.readValue(responsePayload, LinkedHashMap.class);
            var token = (String) deserialized.get("access_token");
            var expiresIn = ((Integer) deserialized.get("expires_in")).longValue();
            var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(token).expiresIn(expiresIn).build();
            return Result.success(tokenRepresentation);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation) {
        return tokenValidationService.validate(tokenRepresentation);
    }
}
