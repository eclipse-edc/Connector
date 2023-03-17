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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.CatalogRequest;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.catalog.types.CatalogRequestMessage;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.util.function.Function;

import static org.eclipse.edc.jsonld.JsonLdUtil.expandDocument;

public class CatalogRequestDelegate implements DspDispatcherDelegate<CatalogRequest, Catalog> {
    
    private ObjectMapper mapper;
    private JsonLdTransformerRegistry transformerRegistry;
    
    public CatalogRequestDelegate(ObjectMapper mapper, JsonLdTransformerRegistry transformerRegistry) {
        this.mapper = mapper;
        this.transformerRegistry = transformerRegistry;
    }
    
    @Override
    public Class<CatalogRequest> getMessageType() {
        return CatalogRequest.class;
    }
    
    @Override
    public Request buildRequest(CatalogRequest message) {
        var catalogRequestMessage = CatalogRequestMessage.Builder.newInstance()
                .filter(message.getQuerySpec())
                .build();
        var requestBody = RequestBody.create(toJson(catalogRequestMessage), MediaType.get(jakarta.ws.rs.core.MediaType.APPLICATION_JSON));
        
        return new Request.Builder()
                .url(message.getConnectorAddress() + "/catalog/request")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();
    }
    
    private String toJson(CatalogRequestMessage message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new EdcException("Failed to serialize catalog request", e);
        }
    }
    
    @Override
    public Function<Response, Catalog> parseResponse() {
        return response -> {
            try {
                var jsonObject = mapper.readValue(response.body().bytes(), JsonObject.class);
                var result = transformerRegistry.transform(expandDocument(jsonObject).get(0), Catalog.class);
                if (result.succeeded()) {
                    return result.getContent();
                } else {
                    throw new EdcException("Failed to read response body.");
                }
            } catch (IOException e) {
                throw new EdcException("Failed to read response body.", e);
            }
        };
    }
}
