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

package org.eclipse.edc.protocol.dsp.http.spi.dispatcher;

import okhttp3.Request;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * Creates an HTTP request for the DSP HTTP Bindings given the message instance
 *
 * @param <M> the message type.
 */
@FunctionalInterface
public interface DspHttpRequestFactory<M extends RemoteMessage> {

    default String removeTrailingSlash(String path) {
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Create the request given the message and a {@link RequestPathProvider}
     *
     * @param message the message.
     * @return the request.
     */
    Request createRequest(M message);
}
