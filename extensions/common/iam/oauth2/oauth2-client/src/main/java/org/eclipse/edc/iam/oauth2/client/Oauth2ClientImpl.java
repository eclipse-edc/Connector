/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.iam.oauth2.client;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusIsNot;

public class Oauth2ClientImpl implements Oauth2Client {

    private static final String ACCEPT = "Accept";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final String APPLICATION_JSON = "application/json";
    private static final String RESPONSE_ACCESS_TOKEN_CLAIM = "access_token";
    private static final String RESPONSE_EXPIRES_IN_CLAIM = "expires_in";

    private final EdcHttpClient httpClient;
    private final TypeManager typeManager;

    public Oauth2ClientImpl(EdcHttpClient httpClient, TypeManager typeManager) {
        this.httpClient = httpClient;
        this.typeManager = typeManager;
    }

    private static Request toRequest(Oauth2CredentialsRequest request) {
        return new Request.Builder()
                .url(request.getUrl())
                .addHeader(ACCEPT, APPLICATION_JSON)
                .addHeader(CONTENT_TYPE, FORM_URLENCODED)
                .post(createRequestBody(request))
                .build();
    }

    private static FormBody createRequestBody(Oauth2CredentialsRequest request) {
        var builder = new FormBody.Builder();
        request.getParams().entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> builder.add(entry.getKey(), entry.getValue().toString()));
        return builder.build();
    }

    @Override
    public Result<TokenRepresentation> requestToken(Oauth2CredentialsRequest request) {
        return httpClient.execute(toRequest(request), List.of(retryWhenStatusIsNot(200)), this::handleResponse);
    }

    private Result<TokenRepresentation> handleResponse(Response response) {
        return getStringBody(response)
                .map(it -> typeManager.readValue(it, Map.class))
                .map(this::mapResponse);
    }

    private TokenRepresentation mapResponse(Map<String, Object> response) {
        var builder = TokenRepresentation.Builder.newInstance();
        builder.token(response.get(RESPONSE_ACCESS_TOKEN_CLAIM).toString());

        Optional.ofNullable(response.get(RESPONSE_EXPIRES_IN_CLAIM))
                .flatMap(expiresIn -> {
                    if (expiresIn instanceof Number n) {
                        return Optional.of(n.longValue());
                    }
                    return Optional.empty();
                })
                .ifPresent(builder::expiresIn);

        var additional = new HashMap<>(response);
        additional.remove(RESPONSE_EXPIRES_IN_CLAIM);
        additional.remove(RESPONSE_ACCESS_TOKEN_CLAIM);

        builder.additional(additional);

        return builder.build();
    }

    @NotNull
    private Result<String> getStringBody(Response response) {
        try (var body = response.body()) {
            if (body != null) {
                return Result.success(body.string());
            } else {
                return Result.failure("Body is null");
            }
        } catch (IOException e) {
            return Result.failure("Cannot read response body as String: " + e.getMessage());
        }

    }

}
