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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;

import java.util.function.Function;

import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.TERMINATION;

/**
 * Delegate for dispatching contract negotiation termination message as defined in the dataspace protocol specification.
 */
public class ContractNegotiationTerminationMessageHttpDelegate implements DspHttpDispatcherDelegate<ContractNegotiationTerminationMessage, Object> {

    private static final String APPLICATION_JSON = "application/json";
    
    private JsonLdRemoteMessageSerializer serializer;

    public ContractNegotiationTerminationMessageHttpDelegate(JsonLdRemoteMessageSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public Class<ContractNegotiationTerminationMessage> getMessageType() {
        return ContractNegotiationTerminationMessage.class;
    }

    /**
     * Sends a contract negotiation termination message. The request body is constructed as defined
     * in the dataspace protocol. The request is sent to the remote component using the path from the
     * http binding.
     *
     * @param message the message.
     * @return the built okhttp request.
     */
    @Override
    public Request buildRequest(ContractNegotiationTerminationMessage message) {
        var body = serializer.serialize(message, jsonLdContext());
        var requestBody = RequestBody.create(body, MediaType.get(APPLICATION_JSON));
    
        return new Request.Builder()
                .url(message.getCallbackAddress() + BASE_PATH + message.getProcessId() + TERMINATION)
                .header("Content-Type", APPLICATION_JSON)
                .post(requestBody)
                .build();
    }

    /**
     * Parses the response to a contract negotiation event message. The JSON-LD structure from the response
     * body is expanded and returned.
     *
     * @return a function that contains the response body or null.
     */
    @Override
    public Function<Response, Object> parseResponse() {
        return response -> null;
    }

    // TODO refactor according to https://github.com/eclipse-edc/Connector/issues/2763
    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .build();
    }
}
