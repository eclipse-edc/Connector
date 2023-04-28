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

import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;

import java.util.function.Function;

import static org.eclipse.edc.protocol.dsp.negotiation.dispatcher.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.dispatcher.NegotiationApiPaths.EVENT;

/**
 * Delegate for dispatching contract negotiation event message as defined in the dataspace protocol specification.
 */
public class ContractNegotiationEventMessageHttpDelegate extends DspHttpDispatcherDelegate<ContractNegotiationEventMessage, Object> {

    public ContractNegotiationEventMessageHttpDelegate(JsonLdRemoteMessageSerializer serializer) {
        super(serializer);
    }

    @Override
    public Class<ContractNegotiationEventMessage> getMessageType() {
        return ContractNegotiationEventMessage.class;
    }

    /**
     * Sends a contract negotiation event message. The request body is constructed as defined in the
     * dataspace protocol. The request is sent to the remote component using the path from the http
     * binding.
     *
     * @param message the message.
     * @return the built okhttp request.
     */
    @Override
    public Request buildRequest(ContractNegotiationEventMessage message) {
        return buildRequest(message, BASE_PATH + message.getProcessId() + EVENT);
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

}
