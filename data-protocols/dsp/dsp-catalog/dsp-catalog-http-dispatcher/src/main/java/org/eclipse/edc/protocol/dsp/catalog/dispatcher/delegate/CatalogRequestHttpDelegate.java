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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.IOException;
import java.util.function.Function;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.edc.protocol.dsp.catalog.dispatcher.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.dispatcher.CatalogApiPaths.CATALOG_REQUEST;

/**
 * Delegate for dispatching catalog requests as defined in the dataspace protocol specification.
 */
public class CatalogRequestHttpDelegate extends DspHttpDispatcherDelegate<CatalogRequestMessage, Catalog> {

    private final ObjectMapper mapper;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonLd jsonLdService;

    public CatalogRequestHttpDelegate(JsonLdRemoteMessageSerializer serializer, ObjectMapper mapper, TypeTransformerRegistry transformerRegistry, JsonLd jsonLdService) {
        super(serializer);
        this.mapper = mapper;
        this.transformerRegistry = transformerRegistry;
        this.jsonLdService = jsonLdService;
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
        return buildRequest(message, BASE_PATH + CATALOG_REQUEST);
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
                var expansion = jsonLdService.expand(jsonObject);

                if (expansion.succeeded()) {
                    var result = transformerRegistry.transform(expansion.getContent(), Catalog.class);
                    if (result.succeeded()) {
                        return result.getContent();
                    } else {
                        throw new EdcException(format("Failed to read response body: %s", join(", ", result.getFailureMessages())));
                    }
                } else {
                    throw new EdcException(expansion.getFailureDetail());
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

}
