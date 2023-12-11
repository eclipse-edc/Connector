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

package org.eclipse.edc.connector.dataplane.api.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.HttpHeaders;
import okhttp3.Request;
import org.eclipse.edc.connector.dataplane.spi.resolver.DataAddressResolver;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.io.IOException;

import static java.lang.String.format;

public class ConsumerPullTransferDataAddressResolver implements DataAddressResolver {

    private final EdcHttpClient httpClient;
    private final String endpoint;
    private final ObjectMapper mapper;

    public ConsumerPullTransferDataAddressResolver(EdcHttpClient httpClient, String endpoint, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.mapper = mapper;
    }

    /**
     * Resolves access token received in input of Data Plane public API (consumer pull) into the {@link DataAddress}
     * of the requested data.
     *
     * @param token Access token received in input of the Data Plane public API
     * @return Data address
     */
    @Override
    public Result<DataAddress> resolve(String token) {
        var request = new Request.Builder().url(endpoint).header(HttpHeaders.AUTHORIZATION, token).get().build();
        try (var response = httpClient.execute(request)) {
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
