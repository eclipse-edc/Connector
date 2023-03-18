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
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementRequest;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.util.function.Function;

import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transform.util.DocumentUtil.compactDocument;

/**
 * Sends dspace:ContractAgreementMessage, expects no response or dspace:ContractNegotiationError.
 */
public class ContractAgreementDelegate implements DspDispatcherDelegate<ContractAgreementRequest, JsonObject> {

    private final ObjectMapper mapper;

    private final JsonLdTransformerRegistry registry;

    public ContractAgreementDelegate(ObjectMapper mapper, JsonLdTransformerRegistry registry) {
        this.mapper = mapper;
        this.registry = registry;
    }

    @Override
    public Class<ContractAgreementRequest> getMessageType() {
        return ContractAgreementRequest.class;
    }

    @Override
    public Request buildRequest(ContractAgreementRequest message) {
        var agreement = registry.transform(message, JsonObject.class);
        if (agreement.failed()) {
            throw new EdcException("Failed to create request body for contract agreement message.");
        }

        var content = mapper.convertValue(compactDocument(agreement.getContent()), JsonObject.class);
        var requestBody = RequestBody.create(toString(content), MediaType.get(jakarta.ws.rs.core.MediaType.APPLICATION_JSON));

        return new Request.Builder()
                .url(message.getConnectorAddress() + BASE_PATH + message.getCorrelationId() + AGREEMENT)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();
    }

    /**
     * Reads response for request message. NOTE: No response is expected.
     *
     * @return null or any object.
     */
    @Override
    public Function<Response, JsonObject> parseResponse() {
        return response -> {
            try {
                var responseBody = response.body();
                if (responseBody == null) {
                    return null;
                }

                return mapper.readValue(responseBody.bytes(), JsonObject.class);
            } catch (RuntimeException | IOException e) {
                throw new EdcException("Failed to read response body from contract agreement request.", e);
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
