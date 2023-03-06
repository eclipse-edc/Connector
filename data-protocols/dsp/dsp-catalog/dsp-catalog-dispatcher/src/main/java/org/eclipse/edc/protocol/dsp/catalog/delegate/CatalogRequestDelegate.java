/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.delegate;

import java.io.IOException;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.catalog.spi.CatalogRequest;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

public class CatalogRequestDelegate implements DspDispatcherDelegate<CatalogRequest, JsonObject> {
    
    private ObjectMapper objectMapper;
    
    public CatalogRequestDelegate(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Class<CatalogRequest> getMessageType() {
        return CatalogRequest.class;
    }
    
    @Override
    public Request buildRequest(CatalogRequest message) {
        //TODO body
        var requestBody = RequestBody.create("content", MediaType.get(jakarta.ws.rs.core.MediaType.APPLICATION_JSON));
        
        return new Request.Builder()
                .url(message.getConnectorAddress() + "/catalog/request")
                .header("Authorization", "") //TODO authentication
                .post(requestBody)
                .build();
    }
    
    @Override
    public Function<Response, JsonObject> parseResponse() {
        return response -> {
            try {
                return objectMapper.readValue(response.body().bytes(), JsonObject.class);
            } catch (IOException e) {
                throw new EdcException("Failed to read response body.", e);
            }
        };
    }
}
