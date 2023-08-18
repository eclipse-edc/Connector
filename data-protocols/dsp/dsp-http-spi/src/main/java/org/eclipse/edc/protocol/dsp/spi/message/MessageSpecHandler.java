/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.message;

import jakarta.ws.rs.core.Response;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * Handles incoming DSP requests
 */
public interface MessageSpecHandler {

    /**
     * Verify identity, call the service, transform the resource and return the response.
     *
     * @param dspRequest the request
     * @return the response to be returned to the client
     * @param <R> the resource type.
     */
    <R> Response getResource(GetDspRequest<R> dspRequest);

    /**
     * Verify identity, validate incoming message, transform, call the service, return the json-ld representation of the
     * resource.
     *
     * @param dspRequest the request.
     * @return the response to be returned to the client.
     * @param <I> the input type.
     * @param <R> the result type.
     */
    <I extends RemoteMessage, R> Response createResource(PostDspRequest<I, R> dspRequest);

    /**
     * Verify identity, validate incoming message, transform and call the service.
     *
     * @param dspRequest the request.
     * @return the response to be returned to the client.
     * @param <I> the input type.
     * @param <R> the result type.
     */
    <I extends RemoteMessage, R> Response updateResource(PostDspRequest<I, R> dspRequest);

    /**
     * Verify identity, validate incoming message, transform and call the service.
     *
     * @param dpsMessage the message description.
     * @return a result containing the object returned by the service.
     * @param <I> the input type.
     * @param <R> the result type.
     */
    <I extends RemoteMessage, R> ServiceResult<R> handlePostRequest(PostDspRequest<I, R> dpsMessage);

}
