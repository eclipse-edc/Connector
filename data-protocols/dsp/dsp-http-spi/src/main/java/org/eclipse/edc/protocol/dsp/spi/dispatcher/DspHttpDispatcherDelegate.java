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

package org.eclipse.edc.protocol.dsp.spi.dispatcher;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Delegate for sending a specific type of {@link RemoteMessage} using the dataspace protocol.
 *
 * @param <M> the type of message
 * @param <R> the response type
 */
public abstract class DspHttpDispatcherDelegate<M extends RemoteMessage, R> {

    private static final String APPLICATION_JSON = "application/json";

    private final JsonLdRemoteMessageSerializer serializer;

    protected DspHttpDispatcherDelegate(JsonLdRemoteMessageSerializer serializer) {
        this.serializer = serializer;
    }

    /**
     * Returns the type of {@link RemoteMessage} this delegate can handle.
     *
     * @return the message type
     */
    public abstract Class<M> getMessageType();

    /**
     * Builds the HTTP request for the message including method, URL, body and headers. The
     * Authorization header can be omitted as it is handled centrally.
     *
     * @param message the message
     * @return the request builder
     */
    public abstract Request buildRequest(M message);

    /**
     * Handles the response and returns a {@link StatusResult} containing the response object
     *
     * @return the {@link StatusResult}
     */
    public Function<Response, StatusResult<R>> handleResponse() {
        return response -> {
            if (response.isSuccessful()) {
                var responsePayload = parseResponse().apply(response);
                return StatusResult.success(responsePayload);
            } else {
                var responseBody = Optional.ofNullable(response.body())
                        .map(this::asString)
                        .orElse("Response body is null");

                var status = response.code() >= 400 && response.code() < 500
                        ? FATAL_ERROR : ERROR_RETRY;

                return StatusResult.failure(status, responseBody);
            }
        };
    }

    /**
     * Parses the response to return an instance of the expected response type.
     *
     * @return the parsed response
     */
    protected abstract Function<Response, R> parseResponse();

    protected Request buildRequest(M message, String path) {
        var body = serializer.serialize(message);
        var requestBody = RequestBody.create(body, MediaType.get(APPLICATION_JSON));

        var url = HttpUrl.get(message.getCounterPartyAddress() + path);

        return new Request.Builder()
                .url(url)
                .header("Content-Type", APPLICATION_JSON)
                .post(requestBody)
                .build();
    }

    private String asString(ResponseBody it) {
        try {
            return it.string();
        } catch (IOException e) {
            return "Cannot read response body: " + e.getMessage();
        }
    }

}
