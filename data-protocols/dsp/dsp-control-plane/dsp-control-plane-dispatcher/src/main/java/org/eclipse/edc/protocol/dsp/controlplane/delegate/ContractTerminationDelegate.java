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
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRejection;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

import java.util.function.Function;

import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.TERMINATION;
import static org.eclipse.edc.protocol.dsp.transform.util.DocumentUtil.compactDocument;

/**
 * Sends dspace:ContractTerminationMessage, expects no response or dspace:ContractNegotiationError.
 */
public class ContractTerminationDelegate implements DspDispatcherDelegate<ContractRejection, JsonObject> {

    private final ObjectMapper mapper;

    private final JsonLdTransformerRegistry registry;

    public ContractTerminationDelegate(ObjectMapper mapper, JsonLdTransformerRegistry registry) {
        this.mapper = mapper;
        this.registry = registry;
    }

    @Override
    public Class<ContractRejection> getMessageType() {
        return ContractRejection.class;
    }

    @Override
    public Request buildRequest(ContractRejection message) {
        var termination = registry.transform(message, JsonObject.class);
        if (termination.failed()) {
            throw new EdcException("Failed to create request body for contract termination message.");
        }

        var content = mapper.convertValue(compactDocument(termination.getContent()), JsonObject.class);
        var requestBody = RequestBody.create(toString(content), MediaType.get(jakarta.ws.rs.core.MediaType.APPLICATION_JSON));

        return new Request.Builder()
                .url(message.getConnectorAddress() + BASE_PATH + message.getCorrelationId() + TERMINATION)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();
    }

    @Override
    public Function<Response, JsonObject> parseResponse() {
        return null;
    }

    private String toString(JsonObject input) {
        try {
            return mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new EdcException("Failed to serialize dspace:ContractTerminationMessage", e);
        }
    }
}
