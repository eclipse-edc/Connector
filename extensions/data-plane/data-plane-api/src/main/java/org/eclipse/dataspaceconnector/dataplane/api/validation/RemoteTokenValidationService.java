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
import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.io.IOException;

/**
 * Implementation of {@link TokenValidationService} that delegates to a remote token validate service.
 */
public class RemoteTokenValidationService implements TokenValidationService {

    private final OkHttpClient httpClient;
    private final String endpoint;
    private final ObjectMapper mapper;

    public RemoteTokenValidationService(OkHttpClient httpClient, String endpoint, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.mapper = mapper;
    }

    @Override
    public Result<ClaimToken> validate(TokenRepresentation tokenRepresentation) {
        var token = tokenRepresentation.getToken();
        var request = new Request.Builder().url(endpoint).header(HttpHeaders.AUTHORIZATION, token).get().build();
        try (var response = httpClient.newCall(request).execute()) {
            var body = response.body();
            var stringBody = body != null ? body.string() : null;
            if (stringBody == null) {
                return Result.failure("Validation server returned null body");
            }

            if (response.isSuccessful()) {
                return Result.success(mapper.readValue(stringBody, ClaimToken.class));
            } else {
                return Result.failure(String.format("Call to validation sever failed: %s - %s. %s", response.code(), response.message(), stringBody));
            }
        } catch (IOException e) {
            return Result.failure("Unhandled exception occured during call to validation server: " + e.getMessage());
        }
    }
}
