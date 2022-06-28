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
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.io.IOException;

/**
 * Client used by the data plane to hit the validation server (that can be hosted by the Control Plane for example).
 * The validation server will assert the validity of the token following a set of rules, and if successful, it will
 * return the decrypted data address contained in the input token.
 */
public class TokenValidationClient {

    private final OkHttpClient httpClient;
    private final String endpoint;
    private final ObjectMapper mapper;
    private final Monitor monitor;

    public TokenValidationClient(OkHttpClient httpClient, String endpoint, ObjectMapper mapper, Monitor monitor) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.mapper = mapper;
        this.monitor = monitor;
    }

    /**
     * Hits the token validation endpoint to verify if the provided token is valid.
     *
     * @param token Token received in input of the data plane.
     * @return Decrypted {@link DataAddress} contained in the input claim token.
     */
    public Result<DataAddress> callTokenValidationServer(String token) {
        monitor.debug("Start call to validation server");

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
                return Result.failure(String.format("Call to token validation sever failed: %s - %s. %s", response.code(), response.message(), stringBody));
            }
        } catch (IOException e) {
            return Result.failure("Unhandled exception occurred during call to token validation server: " + e.getMessage());
        }
    }
}
