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
import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.util.function.Function;

import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;

/**
 * Delegate for dispatching contract request message as defined in the dataspace protocol specification.
 */
public class ContractRequestMessageHttpDelegate extends DspHttpDispatcherDelegate<ContractRequestMessage, Object> {
    
    private ObjectMapper mapper;

    public ContractRequestMessageHttpDelegate(JsonLdRemoteMessageSerializer serializer, ObjectMapper mapper) {
        super(serializer);
        this.mapper = mapper;
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
        if (message.getType() == ContractRequestMessage.Type.INITIAL) {
            return buildRequest(message, BASE_PATH + INITIAL_CONTRACT_REQUEST, jsonLdContext());
        } else {
            return buildRequest(message, BASE_PATH + message.getProcessId() + CONTRACT_REQUEST, jsonLdContext());
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

    // TODO refactor according to https://github.com/eclipse-edc/Connector/issues/2763
    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .add(ODRL_PREFIX, ODRL_SCHEMA)
                .build();
    }
}
