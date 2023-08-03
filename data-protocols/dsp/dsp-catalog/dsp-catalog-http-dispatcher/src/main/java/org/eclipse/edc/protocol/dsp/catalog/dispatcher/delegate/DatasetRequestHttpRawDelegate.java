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

package org.eclipse.edc.protocol.dsp.catalog.dispatcher.delegate;

import jakarta.json.JsonObject;
import okhttp3.Response;
import org.eclipse.edc.catalog.spi.DatasetRequestMessage;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.util.function.Function;

/**
 * Delegate for dispatching catalog requests as defined in the
 * <a href="https://docs.internationaldataspaces.org/dataspace-protocol/catalog/catalog.binding.https">dataspace protocol specification</a>
 */
public class DatasetRequestHttpRawDelegate extends DspHttpDispatcherDelegate<DatasetRequestMessage, byte[]> {

    public DatasetRequestHttpRawDelegate() {
        super();
    }

    /**
     * Parses the response to a byte[]. It cannot return a {@link java.io.InputStream} because the response gets closed
     * by the {@link org.eclipse.edc.spi.http.EdcHttpClient}
     *
     * @return a function that transforms the response body to a {@link JsonObject}.
     */
    @Override
    public Function<Response, byte[]> parseResponse() {
        return response -> {
            try {
                return response.body().bytes();

            } catch (NullPointerException e) {
                throw new EdcException("Failed to read response body, as body was null.", e);
            } catch (IOException e) {
                throw new EdcException("Failed to read response body", e);
            }
        };
    }
}
