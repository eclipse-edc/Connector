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
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;

import java.util.function.Function;

import static org.eclipse.edc.protocol.dsp.negotiation.dispatcher.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.dispatcher.NegotiationApiPaths.CONTRACT_OFFER;

/**
 * Delegate for dispatching contract offer message as defined in the dataspace protocol specification.
 */
public class ContractOfferMessageHttpDelegate extends DspHttpDispatcherDelegate<ContractOfferMessage, Object> {
    
    public ContractOfferMessageHttpDelegate(JsonLdRemoteMessageSerializer serializer) {
        super(serializer);
    }
    
    @Override
    public Class<ContractOfferMessage> getMessageType() {
        return ContractOfferMessage.class;
    }
    
    @Override
    public Request buildRequest(ContractOfferMessage message) {
        return buildPostRequest(message, BASE_PATH + message.getProcessId() + CONTRACT_OFFER);
    }
    
    @Override
    protected Function<Response, Object> parseResponse() {
        return response -> null;
    }
}
