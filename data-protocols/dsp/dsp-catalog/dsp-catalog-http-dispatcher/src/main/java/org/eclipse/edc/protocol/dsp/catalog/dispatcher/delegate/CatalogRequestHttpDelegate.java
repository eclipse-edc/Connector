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

package org.eclipse.edc.protocol.dsp.catalog.dispatcher.delegate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.CatalogRequest;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;

import java.io.IOException;
import java.util.function.Function;

import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;

/**
 * Delegate for dispatching catalog requests as defined in the dataspace protocol implementation.
 */
public class CatalogRequestHttpDelegate implements DspHttpDispatcherDelegate<CatalogRequest, Catalog> {
    
    private static final String APPLICATION_JSON = "application/json";
    
    //TODO to be removed once #2685 is merged
    static final String BASE_PATH = "/catalog";
    static final String CATALOG_REQUEST = "/request";
    
    private ObjectMapper mapper;
    private JsonLdTransformerRegistry transformerRegistry;
    
    public CatalogRequestHttpDelegate(ObjectMapper mapper, JsonLdTransformerRegistry transformerRegistry) {
        this.mapper = mapper;
        this.transformerRegistry = transformerRegistry;
    }
    
    @Override
    public Class<CatalogRequest> getMessageType() {
        return CatalogRequest.class;
    }
    
    /**
     * Sends a catalog request. The request body is constructed as defined in the dataspace protocol
     * implementation. The request is sent to the remote component using the path from the
     * specification.
     *
     * @param message the message
     * @return the built okhttp request
     */
    @Override
    public Request buildRequest(CatalogRequest message) {
        var catalogRequestMessage = CatalogRequestMessage.Builder.newInstance()
                .filter(message.getQuerySpec())
                .build();
        var requestBody = RequestBody.create(toJson(catalogRequestMessage), MediaType.get(APPLICATION_JSON));
        
        return new Request.Builder()
                .url(message.getConnectorAddress() + BASE_PATH + CATALOG_REQUEST)
                .header("Content-Type", APPLICATION_JSON)
                .post(requestBody)
                .build();
    }
    
    private String toJson(CatalogRequestMessage message) {
        try {
            var transformResult = transformerRegistry.transform(message, JsonObject.class);
            if (transformResult.succeeded()) {
                return mapper.writeValueAsString(transformResult.getContent());
            }
            throw new EdcException("Failed to write request.");
        } catch (JsonProcessingException e) {
            throw new EdcException("Failed to serialize catalog request", e);
        }
    }
    
    /**
     * Parses the response to a catalog request. The JSON-LD structure from the response body is
     * expanded and then transformed to an EDC catalog.
     *
     * @return a function that transforms the response body to a catalog.
     */
    @Override
    public Function<Response, Catalog> parseResponse() {
        return response -> {
            try {
                var jsonObject = mapper.readValue(response.body().bytes(), JsonObject.class);
                var result = transformerRegistry.transform(expand(jsonObject).get(0), Catalog.class);
                if (result.succeeded()) {
                    return result.getContent();
                } else {
                    throw new EdcException("Failed to read response body.");
                }
            } catch (NullPointerException e) {
                throw new EdcException("Failed to read response body, as body was null.");
            } catch (IndexOutOfBoundsException e) {
                throw new EdcException("Failed to expand JSON-LD in response body.", e);
            } catch (IOException e) {
                throw new EdcException("Failed to read response body.", e);
            }
        };
    }
    
    //TODO to be removed once #2685 is merged
    static class CatalogRequestMessage {
        private QuerySpec filter;
    
        public QuerySpec getFilter() {
            return filter;
        }
    
        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder {
            private final CatalogRequestMessage request;
        
            private Builder() {
                request = new CatalogRequestMessage();
            }
        
            @JsonCreator
            public static Builder newInstance() {
                return new Builder();
            }
        
            public Builder filter(QuerySpec filter) {
                request.filter = filter;
                return this;
            }
        
            public CatalogRequestMessage build() {
                return request;
            }
        }
    }
}
