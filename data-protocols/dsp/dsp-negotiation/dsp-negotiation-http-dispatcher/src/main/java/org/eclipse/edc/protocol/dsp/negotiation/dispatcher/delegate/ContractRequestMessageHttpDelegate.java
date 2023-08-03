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

package org.eclipse.edc.protocol.dsp.negotiation.dispatcher.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import okhttp3.Response;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.util.function.Function;

/**
 * Delegate for dispatching contract request message as defined in the dataspace protocol specification.
 */
public class ContractRequestMessageHttpDelegate extends DspHttpDispatcherDelegate<ContractRequestMessage, Object> {

    private final ObjectMapper mapper;
    private final JsonLd jsonLdService;


    public ContractRequestMessageHttpDelegate(JsonLdRemoteMessageSerializer serializer, ObjectMapper mapper, JsonLd jsonLdService) {
        super();
        this.mapper = mapper;
        this.jsonLdService = jsonLdService;
    }

    @Override
    public Function<Response, Object> parseResponse() {
        return response -> {
            try {
                var jsonObject = mapper.readValue(response.body().bytes(), JsonObject.class);
                return jsonLdService.expand(jsonObject).map(jo -> jo).orElseThrow(failure -> new EdcException(failure.getFailureDetail()));
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
