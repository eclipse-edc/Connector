/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.dsp.http.spi.message;

import jakarta.ws.rs.core.Response;

/**
 * Describe a decorator to be used on a {@link Response}
 */
@FunctionalInterface
public interface ResponseDecorator<I, O> {

    /**
     * Decorate the response builder.
     *
     * @param responseBuilder the response builder.
     * @param requestBody the object contained in the request.
     * @param responseBody the object contained in the response.
     * @return the decorated ResponseBuilder.
     */
    Response.ResponseBuilder decorate(Response.ResponseBuilder responseBuilder, I requestBody, O responseBody);
}
