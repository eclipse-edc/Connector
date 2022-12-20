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
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

import static java.lang.String.format;

public class Oauth2ClientImpl implements Oauth2Client {

    private static final String ACCEPT = "Accept";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final String APPLICATION_JSON = "application/json";
    private static final String RESPONSE_ACCESS_TOKEN_CLAIM = "access_token";

    private final EdcHttpClient httpClient;
    private final TypeManager typeManager;
    private final Monitor monitor;

    public Oauth2ClientImpl(EdcHttpClient httpClient, TypeManager typeManager, Monitor monitor) {
        this.httpClient = httpClient;
        this.typeManager = typeManager;
        this.monitor = monitor;
    }

    @Override
    public Result<TokenRepresentation> requestToken(Oauth2CredentialsRequest request) {
        try (var response = httpClient.execute(toRequest(request))) {
            var stringBody = getStringBody(response);
            if (!response.isSuccessful()) {
                return failure(request.getUrl(), format("Server responded %s - %s at the client_credentials request", response.code(), stringBody));
            }

            var responseBody = typeManager.readValue(stringBody, Map.class);
            var token = responseBody.get(RESPONSE_ACCESS_TOKEN_CLAIM).toString();
            return Result.success(TokenRepresentation.Builder.newInstance().token(token).build());
        } catch (IOException e) {
            return failure(request.getUrl(), request.getGrantType() + " request failed", e);
        }
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
        request.getParams().forEach(builder::add);
        return builder.build();
    }

    @NotNull
    private static String getStringBody(Response response) throws IOException {
        var body = response.body();
        if (body != null) {
            return body.string();
        } else {
            return "";
        }
    }

    @NotNull
    private Result<TokenRepresentation> failure(String url, String message) {
        return Result.failure(failureMessage(url, message));
    }

    @NotNull
    private Result<TokenRepresentation> failure(String url, String message, Exception e) {
        var fullMessage = failureMessage(url, message);
        monitor.severe(fullMessage, e);
        return Result.failure(fullMessage);
    }

    @NotNull
    private String failureMessage(String url, String message) {
        return format("Error requesting oauth2 token from %s: %s", url, message);
    }
}
