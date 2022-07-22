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

package org.eclipse.dataspaceconnector.dataplane.api.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.HttpHeaders;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.dataplane.spi.api.TokenValidationClient;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.io.IOException;

import static java.lang.String.format;

public class TokenValidationClientImpl implements TokenValidationClient {

    private final OkHttpClient httpClient;
    private final String endpoint;
    private final ObjectMapper mapper;
    private final Monitor monitor;

    public TokenValidationClientImpl(OkHttpClient httpClient, String endpoint, ObjectMapper mapper, Monitor monitor) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.mapper = mapper;
        this.monitor = monitor;
    }

    @Override
    public Result<DataAddress> call(String token) {
        var request = new Request.Builder().url(endpoint).header(HttpHeaders.AUTHORIZATION, token).get().build();
        try (var response = httpClient.newCall(request).execute()) {
            var body = response.body();
            var stringBody = body != null ? body.string() : null;
            if (stringBody == null) {
                return Result.failure("Token validation server returned null body");
            }

            if (response.isSuccessful()) {
                return Result.success(mapper.readValue(stringBody, DataAddress.class));
            } else {
                return Result.failure(format("Call to token validation sever failed: %s - %s. %s", response.code(), response.message(), stringBody));
            }
        } catch (IOException e) {
            return Result.failure("Unhandled exception occurred during call to token validation server: " + e.getMessage());
        }
    }
}
