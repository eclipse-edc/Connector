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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.util.function.Function;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.compact;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;
import static org.eclipse.edc.protocol.dsp.catalog.spi.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.spi.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_SCHEMA;

/**
 * Delegate for dispatching catalog requests as defined in the dataspace protocol specification.
 */
public class CatalogRequestHttpDelegate implements DspHttpDispatcherDelegate<CatalogRequestMessage, Catalog> {

    private static final String APPLICATION_JSON = "application/json";

    private final ObjectMapper mapper;
    private final JsonLdTransformerRegistry transformerRegistry;

    public CatalogRequestHttpDelegate(ObjectMapper mapper, JsonLdTransformerRegistry transformerRegistry) {
        this.mapper = mapper;
        this.transformerRegistry = transformerRegistry;
    }

    @Override
    public Class<CatalogRequestMessage> getMessageType() {
        return CatalogRequestMessage.class;
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
    public Request buildRequest(CatalogRequestMessage message) {
        var catalogRequestMessage = org.eclipse.edc.catalog.spi.protocol.CatalogRequestMessage.Builder.newInstance()
                .filter(message.getQuerySpec())
                .build();
        var requestBody = RequestBody.create(toJson(catalogRequestMessage), MediaType.get(APPLICATION_JSON));

        return new Request.Builder()
                .url(message.getCallbackAddress() + BASE_PATH + CATALOG_REQUEST)
                .post(requestBody)
                .build();
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
                    throw new EdcException(format("Failed to read response body: %s", join(", ", result.getFailureMessages())));
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

    private String toJson(org.eclipse.edc.catalog.spi.protocol.CatalogRequestMessage message) {
        try {
            var transformResult = transformerRegistry.transform(message, JsonObject.class);
            if (transformResult.succeeded()) {
                var compacted = compact(transformResult.getContent(), jsonLdContext());
                return mapper.writeValueAsString(compacted);
            }
            throw new EdcException(format("Failed to write request: %s", join(", ", transformResult.getFailureMessages())));
        } catch (JsonProcessingException e) {
            throw new EdcException("Failed to serialize catalog request", e);
        }
    }
    
    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .build();
    }
}
