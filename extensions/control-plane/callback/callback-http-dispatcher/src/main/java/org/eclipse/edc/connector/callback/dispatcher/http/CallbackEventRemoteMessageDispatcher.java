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

package org.eclipse.edc.connector.callback.dispatcher.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.controlplane.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;

import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;

/**
 * Implementation of {@link GenericHttpDispatcherDelegate} that works for message of type {@link CallbackEventRemoteMessage}
 */
public class CallbackEventRemoteMessageDispatcher implements GenericHttpDispatcherDelegate<CallbackEventRemoteMessage, Void> {

    private static final String APPLICATION_JSON = "application/json";
    private final ObjectMapper mapper;

    private final Vault vault;

    public CallbackEventRemoteMessageDispatcher(ObjectMapper mapper, Vault vault) {
        this.mapper = mapper;
        this.vault = vault;
    }

    @Override
    public Class<CallbackEventRemoteMessage> getMessageType() {
        return CallbackEventRemoteMessage.class;
    }

    @Override
    public Request buildRequest(CallbackEventRemoteMessage message) {
        try {
            var eventName = message.getEventEnvelope().getPayload().name();
            var body = mapper.writeValueAsString(((CallbackEventRemoteMessage<?>) message).getEventEnvelope());

            var builder = new Request.Builder()
                    .url(message.getCounterPartyAddress())
                    .post((RequestBody.create(body, MediaType.get(APPLICATION_JSON))));

            if (message.getAuthKey() != null) {
                var authCode = extractAuthCode(eventName, message.getAuthCodeId());
                builder.addHeader(message.getAuthKey(), authCode);
            }
            return builder.build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Function<Response, Void> parseResponse() {
        return response -> {
            if (response.isSuccessful()) {
                return null;
            } else {
                throw new EdcException(format("Received error code %s when calling the callback endpoint at uri: %s", response.code(), response.request().url().url().toString()));
            }
        };
    }

    private String extractAuthCode(String eventName, String authCodeId) {
        if (authCodeId == null) {
            throw new EdcException(format("Error dispatching event %s: Auth Code Id cannot be null when the Auth Key was provided", eventName));
        }
        return Optional.ofNullable(vault.resolveSecret(authCodeId))
                .orElseThrow(() -> new EdcException(format("Error dispatching event %s: no secret found in vault with name %s", eventName, authCodeId)));
    }
}
