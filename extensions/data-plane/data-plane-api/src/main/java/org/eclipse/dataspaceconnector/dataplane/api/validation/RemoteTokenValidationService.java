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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Implementation of {@link TokenValidationService} that delegates to a remote token validate service.
 */
public class RemoteTokenValidationService implements TokenValidationService {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final OkHttpClient httpClient;
    private final String remoteValidationEndpoint;
    private final ObjectMapper mapper;

    public RemoteTokenValidationService(OkHttpClient httpClient, String controlPlaneValidationEndpoint, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.remoteValidationEndpoint = controlPlaneValidationEndpoint;
        this.mapper = mapper;
    }

    @Override
    public Result<ClaimToken> validate(@NotNull String token) {
        var request = new Request.Builder().url(remoteValidationEndpoint).header(AUTHORIZATION_HEADER, token).get().build();
        try (var response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                var body = response.body();
                if (body == null) {
                    return Result.failure("Received null body from validation server");
                }
                return Result.success(mapper.readValue(body.string(), ClaimToken.class));
            }
            return Result.failure(String.format("Call to validation facade was not successful: %s - %s", response.code(), response.message()));
        } catch (IOException e) {
            return Result.failure("Unhandled exception occurred when calling validation server: " + e.getMessage());
        }
    }
}
