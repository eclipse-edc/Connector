/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.provision.http.port;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.dataplane.spi.provision.DeprovisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.response.StatusResult;

import java.io.IOException;

import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

public class ProvisionHttpClient {

    private final String callbackAddress;
    private final EdcHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ProvisionHttpClient(String callbackAddress, EdcHttpClient httpClient, ObjectMapper objectMapper) {
        this.callbackAddress = callbackAddress;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public StatusResult<ProvisionedResource> provision(ProvisionResource provisionResource) {
        return createRequest("provision", provisionResource)
                    .compose(this::call)
                    .map(v -> ProvisionedResource.Builder.from(provisionResource).pending(true).build()
        );
    }

    public StatusResult<DeprovisionedResource> deprovision(ProvisionResource provisionResource) {
        return createRequest("deprovision", provisionResource)
                .compose(this::call)
                .map(v -> DeprovisionedResource.Builder.from(provisionResource).pending(true).build());
    }

    private StatusResult<Request> createRequest(String type, ProvisionResource provisionResource) {
        try {
            var provisionHttpRequest = new ProvisionHttpRequest(type, provisionResource.getId(), provisionResource.getFlowId(), callbackAddress);
            var requestBody = RequestBody.create(objectMapper.writeValueAsString(provisionHttpRequest), MediaType.get("application/json"));
            var request = new Request.Builder().url(provisionResource.getProperty("endpoint").toString()).post(requestBody).build();
            return StatusResult.success(request);
        } catch (Throwable e) {
            return StatusResult.failure(FATAL_ERROR, "Fatal error serializing provision http request: " + e.getMessage());
        }
    }

    private StatusResult<Void> call(Request request) {
        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                return StatusResult.success();
            } else if (response.code() >= 500 && response.code() <= 504) {
                return StatusResult.failure(ERROR_RETRY, "ProvisionHttp: received error code: " + response.code());
            } else {
                return StatusResult.failure(FATAL_ERROR, "ProvisionHttp: received fatal error code: " + response.code());
            }
        } catch (IOException e) {
            return StatusResult.failure(ERROR_RETRY, "ProvisionHttp: received error: " + e.getMessage());
        }
    }

}
