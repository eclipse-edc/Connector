/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.http.client;

import okhttp3.Request;
import org.eclipse.edc.api.auth.spi.ControlClientAuthenticationProvider;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.http.spi.EdcHttpClientException;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;
import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusIsNotIn;

public class ControlApiHttpClientImpl implements ControlApiHttpClient {

    private final EdcHttpClient httpClient;
    private final ControlClientAuthenticationProvider authenticationProvider;

    public ControlApiHttpClientImpl(EdcHttpClient httpClient, ControlClientAuthenticationProvider authenticationProvider) {
        this.httpClient = httpClient;
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    public ServiceResult<Void> execute(Request.Builder requestBuilder) {
        return request(requestBuilder).mapEmpty();
    }

    @Override
    public ServiceResult<String> request(Request.Builder requestBuilder) {
        authenticationProvider.authenticationHeaders().forEach(requestBuilder::header);
        try (
                var response = httpClient.execute(requestBuilder.build(), List.of(retryWhenStatusIsNotIn(200, 202, 204)));
                var responseBody = response.body();
        ) {
            if (response.isSuccessful()) {
                return ServiceResult.success(responseBody.string());
            } else {
                return mapToFailure(response.code(), responseBody.string());
            }
        } catch (IOException exception) {
            return ServiceResult.unexpected("Unexpected IOException. " + exception.getMessage());
        } catch (EdcHttpClientException exception) {
            return mapToFailure(exception.getStatusCode(), exception.getResponseBody());
        }
    }

    private @NotNull ServiceResult<String> mapToFailure(int statusCode, String responseBody) {
        return switch (statusCode) {
            case 400 -> ServiceResult.badRequest("Remote API returned HTTP 400. " + responseBody);
            case 401, 403 -> ServiceResult.unauthorized("Unauthorized. " + responseBody);
            case 404 -> ServiceResult.notFound("Remote API returned HTTP 404. " + responseBody);
            case 409 -> ServiceResult.conflict("Remote API returned HTTP 409. " + responseBody);
            default ->
                    ServiceResult.unexpected(format("An unknown error happened, HTTP Status = %d. Body %s", statusCode, responseBody));
        };
    }

}
