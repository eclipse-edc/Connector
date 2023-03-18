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

package org.eclipse.edc.protocol.dsp.controlplane.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.util.function.Function;

import static org.eclipse.edc.jsonld.JsonLdUtil.expandDocument;
import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.INITIAL_CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.transform.util.DocumentUtil.compactDocument;

/**
 * Sends dspace:ContractRequestMessage, expects dspace:ContractNegotiation.
 */
public class ContractRequestDelegate implements DspDispatcherDelegate<ContractOfferRequest, ContractNegotiation> {

    private final ObjectMapper mapper;

    private final JsonLdTransformerRegistry registry;

    public ContractRequestDelegate(ObjectMapper mapper, JsonLdTransformerRegistry registry) {
        this.mapper = mapper;
        this.registry = registry;
    }

    @Override
    public Class<ContractOfferRequest> getMessageType() {
        return ContractOfferRequest.class;
    }

    @Override
    public Request buildRequest(ContractOfferRequest message) {
        var contractRequest = registry.transform(message, JsonObject.class);
        if (contractRequest.failed()) {
            throw new EdcException("Failed to create request body for contract request message.");
        }

        var content = mapper.convertValue(compactDocument(contractRequest.getContent()), JsonObject.class);
        var requestBody = RequestBody.create(toString(content), MediaType.get(jakarta.ws.rs.core.MediaType.APPLICATION_JSON));

        if (message.getType() == ContractOfferRequest.Type.INITIAL) {
            return new Request.Builder()
                    .url(message.getConnectorAddress() + BASE_PATH + INITIAL_CONTRACT_REQUEST)
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();
        } else {
            return new Request.Builder()
                    .url(message.getConnectorAddress() + BASE_PATH + message.getCorrelationId() + CONTRACT_REQUEST)
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();
        }
    }

    @Override
    public Function<Response, ContractNegotiation> parseResponse() {
        return response -> {
            try {
                var jsonObject = mapper.readValue(response.body().bytes(), JsonObject.class);
                var result = registry.transform(expandDocument(jsonObject).get(0), ContractNegotiation.class);
                if (result.succeeded()) {
                    return result.getContent();
                } else {
                    throw new EdcException("Failed to read response body from contract request.");
                }
            } catch (RuntimeException | IOException e) {
                throw new EdcException("Failed to read response body from contract request.", e);
            }
        };
    }

    private String toString(JsonObject input) {
        try {
            return mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new EdcException("Failed to serialize dspace:ContractRequestMessage", e);
        }
    }
}
