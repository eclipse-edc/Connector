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
import org.eclipse.edc.participantcontext.spi.types.ParticipantEvent;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.CallbackAddresses;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.jetbrains.annotations.Nullable;

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
                var authCode = resolveAuthCode(callbackAddress, eventEnvelope.getPayload());
                builder.addHeader(callbackAddress.getAuthKey(), authCode);
            }
            return builder.build();
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }

    private String resolveAuthCode(CallbackAddress callbackAddress, Event event) {
        var eventName = event.name();
        var authCodeId = callbackAddress.getAuthCodeId();
        if (authCodeId == null) {
            throw new EdcException(format("Error dispatching event %s: Auth Code Id cannot be null when the Auth Key was provided", eventName));
        }

        var vaultPartition = resolveVaultPartition(callbackAddress, event);
        var authCode = vaultPartition == null ? vault.resolveSecret(authCodeId) : vault.resolveSecret(vaultPartition, authCodeId);
        return Optional.ofNullable(authCode)
                .orElseThrow(() -> new EdcException(format("Error dispatching event %s: no secret found in vault with name %s", eventName, authCodeId)));
    }

    /**
     * Callback addresses supplied dynamically by a participant (e.g. within a transfer or negotiation request) must resolve
     * their secrets from the vault partition of the participant context that owns the event, so that a participant cannot
     * reference secrets that belong to the runtime or to other participants. Statically configured callbacks resolve their
     * secrets from the default vault partition.
     *
     * @return the participant context id to be used as vault partition, or null for the default partition.
     */
    private @Nullable String resolveVaultPartition(CallbackAddress callbackAddress, Event event) {
        if (event instanceof CallbackAddresses dynamicCallbacks && event instanceof ParticipantEvent participantEvent) {
            var isDynamic = dynamicCallbacks.getCallbackAddresses().stream().anyMatch(it -> it == callbackAddress);
            if (isDynamic) {
                return participantEvent.getParticipantContextId();
            }
        }
        return null;
    }
}
