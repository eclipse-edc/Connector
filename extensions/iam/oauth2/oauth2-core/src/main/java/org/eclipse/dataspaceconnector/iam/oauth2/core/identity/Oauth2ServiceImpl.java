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
 *       Fraunhofer Institute for Software and Systems Engineering
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core.identity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.iam.oauth2.core.Oauth2Configuration;
import org.eclipse.dataspaceconnector.iam.oauth2.core.rule.Oauth2ValidationRule;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.JwtDecoratorRegistry;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2Service;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.iam.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.token.JwtValidationRule;
import org.eclipse.dataspaceconnector.token.TokenValidationServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Implements the OAuth2 client credentials flow and bearer token validation.
 */
public class Oauth2ServiceImpl implements Oauth2Service {

    private static final String GRANT_TYPE = "client_credentials";
    private static final String ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final Oauth2Configuration configuration;
    private final OkHttpClient httpClient;
    private final TypeManager typeManager;
    private final JWSSigner tokenSigner;
    private final JwtDecoratorRegistry jwtDecoratorRegistry;
    private final JWSAlgorithm jwsAlgorithm;

    private List<JwtValidationRule> validationRules;
    private TokenValidationService  tokenValidationService;

    /**
     * Creates a new instance of the OAuth2 Service
     *
     * @param configuration             The configuration
     * @param tokenSigner               A {@link JWSSigner} instance.
     * @param client                    Http client
     * @param jwtDecoratorRegistry      Registry containing the decorator for build the JWT
     * @param typeManager               Type manager
     */
    public Oauth2ServiceImpl(Oauth2Configuration configuration, JWSSigner tokenSigner, OkHttpClient client, JwtDecoratorRegistry jwtDecoratorRegistry, TypeManager typeManager) {
        this.configuration = configuration;
        this.typeManager = typeManager;
        httpClient = client;
        this.jwtDecoratorRegistry = jwtDecoratorRegistry;
        this.tokenSigner = tokenSigner;

        //OAuth2 validation must ALWAYS be done
        validationRules = new ArrayList<>();
        validationRules.add(new Oauth2ValidationRule(this.configuration));
        this.tokenValidationService = new TokenValidationServiceImpl(configuration.getIdentityProviderKeyResolver(), Collections.unmodifiableList(validationRules));

        if (tokenSigner instanceof ECDSASigner) {
            jwsAlgorithm = JWSAlgorithm.ES256;
        } else {
            jwsAlgorithm = JWSAlgorithm.RS256;
        }
    }

    @Override
    public void addAdditionalValidationRule(JwtValidationRule additionalValidationRule) {
        validationRules.add(additionalValidationRule);
        this.tokenValidationService = new TokenValidationServiceImpl(configuration.getIdentityProviderKeyResolver(), Collections.unmodifiableList(validationRules));
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(String scope) {
        String assertion = buildJwt();

        RequestBody requestBody = new FormBody.Builder()
                .add("client_assertion_type", ASSERTION_TYPE)
                .add("grant_type", GRANT_TYPE)
                .add("client_assertion", assertion)
                .add("scope", scope)
                .build();

        Request request = new Request.Builder().url(configuration.getTokenUrl()).addHeader("Content-Type", CONTENT_TYPE).post(requestBody).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                try (var body = response.body()) {
                    String message = body == null ? "<empty body>" : body.string();
                    return Result.failure(message);
                }
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return Result.failure("<empty token body>");
            }

            String responsePayload = responseBody.string();
            LinkedHashMap<String, Object> deserialized = typeManager.readValue(responsePayload, LinkedHashMap.class);
            String token = (String) deserialized.get("access_token");
            long expiresIn = ((Integer) deserialized.get("expires_in")).longValue();
            TokenRepresentation tokenRepresentation = TokenRepresentation.Builder.newInstance().token(token).expiresIn(expiresIn).build();
            return Result.success(tokenRepresentation);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation) {
        return tokenValidationService.validate(tokenRepresentation);
    }

    private String buildJwt() {
        try {
            var jwsHeaderBuilder = new JWSHeader.Builder(jwsAlgorithm);
            var claimsSet = new JWTClaimsSet.Builder();
            jwtDecoratorRegistry.getAll().forEach(d -> d.decorate(jwsHeaderBuilder, claimsSet));
            var jwt = new SignedJWT(jwsHeaderBuilder.build(), claimsSet.build());
            jwt.sign(tokenSigner);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }
}
