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
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

import java.util.function.Function;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.compact;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.BASE_PATH;

/**
 * Delegate for dispatching contract agreement message as defined in the dataspace protocol specification.
 */
public class ContractAgreementMessageHttpDelegate implements DspHttpDispatcherDelegate<ContractAgreementMessage, Object> {

    private static final String APPLICATION_JSON = "application/json";

    private final ObjectMapper mapper;
    private final JsonLdTransformerRegistry transformerRegistry;

    public ContractAgreementMessageHttpDelegate(ObjectMapper mapper, JsonLdTransformerRegistry transformerRegistry) {
        this.mapper = mapper;
        this.transformerRegistry = transformerRegistry;
    }

    @Override
    public Class<ContractAgreementMessage> getMessageType() {
        return ContractAgreementMessage.class;
    }

    /**
     * Sends a contract agreement message. The request body is constructed as defined in the dataspace
     * protocol. The request is sent to the remote component using the path from the http binding.
     *
     * @param message the message.
     * @return the built okhttp request.
     */
    @Override
    public Request buildRequest(ContractAgreementMessage message) {
        var requestBody = RequestBody.create(toJson(message), MediaType.get(APPLICATION_JSON));
        return new Request.Builder()
                .url(message.getCallbackAddress() + BASE_PATH + message.getProcessId() + AGREEMENT)
                .header("Content-Type", APPLICATION_JSON)
                .post(requestBody)
                .build();
    }

    /**
     * Parses the response to an agreement message. The JSON-LD structure from the response body is
     * expanded and returned.
     *
     * @return a function that contains the response body or null.
     */
    @Override
    public Function<Response, Object> parseResponse() {
        return response -> null;
    }

    private String toJson(ContractAgreementMessage message) {
        try {
            var transformResult = transformerRegistry.transform(message, JsonObject.class);
            if (transformResult.succeeded()) {
                var compacted = compact(transformResult.getContent(), jsonLdContext());
                return mapper.writeValueAsString(compacted);
            }
            throw new EdcException(format("Failed to write request: %s", join(", ", transformResult.getFailureMessages())));
        } catch (JsonProcessingException e) {
            throw new EdcException("Failed to serialize agreement message", e);
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
