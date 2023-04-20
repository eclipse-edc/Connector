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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.util.function.Function;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.compact;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;

/**
 * Delegate for dispatching contract request message as defined in the dataspace protocol specification.
 */
public class ContractRequestMessageHttpDelegate implements DspHttpDispatcherDelegate<ContractRequestMessage, Object> {

    private static final String APPLICATION_JSON = "application/json";

    private final ObjectMapper mapper;
    private final JsonLdTransformerRegistry transformerRegistry;

    public ContractRequestMessageHttpDelegate(ObjectMapper mapper, JsonLdTransformerRegistry transformerRegistry) {
        this.mapper = mapper;
        this.transformerRegistry = transformerRegistry;
    }

    @Override
    public Class<ContractRequestMessage> getMessageType() {
        return ContractRequestMessage.class;
    }

    /**
     * Sends a contract request message. The request body is constructed as defined in the dataspace
     * protocol. The request is sent to the remote component using the path from the http binding.
     *
     * @param message the message.
     * @return the built okhttp request.
     */
    @Override
    public Request buildRequest(ContractRequestMessage message) {
        var requestBody = RequestBody.create(toJson(message), MediaType.get(APPLICATION_JSON));
        if (message.getType() == ContractRequestMessage.Type.INITIAL) {
            return new Request.Builder()
                    .url(message.getCallbackAddress() + BASE_PATH + INITIAL_CONTRACT_REQUEST)
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();
        } else {
            return new Request.Builder()
                    .url(message.getCallbackAddress() + BASE_PATH + message.getProcessId() + CONTRACT_REQUEST)
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();
        }
    }

    @Override
    public Function<Response, Object> parseResponse() {
        return response -> {
            try {
                var jsonObject = mapper.readValue(response.body().bytes(), JsonObject.class);
                return expand(jsonObject).get(0);
            } catch (NullPointerException e) {
                throw new EdcException("Failed to read response body, as body was null.");
            } catch (IndexOutOfBoundsException e) {
                throw new EdcException("Failed to expand JSON-LD in response body.", e);
            } catch (IOException e) {
                throw new EdcException("Failed to read response body.", e);
            }
        };
    }

    private String toJson(ContractRequestMessage message) {
        try {
            var transformResult = transformerRegistry.transform(message, JsonObject.class);
            if (transformResult.succeeded()) {
                var compacted = compact(transformResult.getContent(), jsonLdContext());
                return mapper.writeValueAsString(compacted);
            }
            throw new EdcException(format("Failed to write request: %s", join(", ", transformResult.getFailureMessages())));
        } catch (JsonProcessingException e) {
            throw new EdcException("Failed to serialize contract request message", e);
        }
    }

    // TODO refactor according to https://github.com/eclipse-edc/Connector/issues/2763
    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .add(ODRL_PREFIX, ODRL_SCHEMA)
                .build();
    }
}
