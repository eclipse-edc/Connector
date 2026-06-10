/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.callback.dispatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackClient;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static java.lang.String.format;

public class CallbackHttpClient implements CallbackClient {

    private static final String APPLICATION_JSON = "application/json";

    private final EdcHttpClient httpClient;
    private final ObjectMapper mapper;
    private final Vault vault;

    public CallbackHttpClient(EdcHttpClient httpClient, ObjectMapper mapper, Vault vault) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.vault = vault;
    }

    @Override
    public <E extends Event> void dispatch(CallbackAddress callbackAddress, EventEnvelope<E> eventEnvelope) {
        var request = buildRequest(callbackAddress, eventEnvelope);

        try (var response = httpClient.execute(request, Collections.emptyList())) {
            if (!response.isSuccessful()) {
                throw new EdcException(format("Received error code %s when calling the callback endpoint at uri: %s",
                        response.code(), response.request().url().url()));
            }
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    private <E extends Event> Request buildRequest(CallbackAddress callbackAddress, EventEnvelope<E> eventEnvelope) {
        try {
            var body = mapper.writeValueAsString(eventEnvelope);
            var builder = new Request.Builder()
                    .url(callbackAddress.getUri())
                    .post(RequestBody.create(body, MediaType.get(APPLICATION_JSON)));

            if (callbackAddress.getAuthKey() != null) {
                var authCode = resolveAuthCode(eventEnvelope.getPayload().name(), callbackAddress.getAuthCodeId());
                builder.addHeader(callbackAddress.getAuthKey(), authCode);
            }
            return builder.build();
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }

    private String resolveAuthCode(String eventName, String authCodeId) {
        if (authCodeId == null) {
            throw new EdcException(format("Error dispatching event %s: Auth Code Id cannot be null when the Auth Key was provided", eventName));
        }
        return Optional.ofNullable(vault.resolveSecret(authCodeId))
                .orElseThrow(() -> new EdcException(format("Error dispatching event %s: no secret found in vault with name %s", eventName, authCodeId)));
    }
}
