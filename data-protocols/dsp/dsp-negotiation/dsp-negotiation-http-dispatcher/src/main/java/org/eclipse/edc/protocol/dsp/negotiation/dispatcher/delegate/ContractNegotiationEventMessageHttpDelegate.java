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

import okhttp3.Response;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;

import java.util.function.Function;

/**
 * Delegate for dispatching contract negotiation event message as defined in the dataspace protocol specification.
 */
public class ContractNegotiationEventMessageHttpDelegate extends DspHttpDispatcherDelegate<Object> {

    public ContractNegotiationEventMessageHttpDelegate() {
        super();
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
