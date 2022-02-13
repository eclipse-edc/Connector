/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.protocols.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.inline.StreamContext;
import org.eclipse.dataspaceconnector.spi.transfer.inline.StreamSession;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.common.ProtocolsSecretToken;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.http.HttpStreamSession;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.ws.WsPushStreamSession;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URI;

import static java.lang.String.format;

/**
 * Implements a push stream context that can create WebSocket-based and HTTP-based sessions.
 */
public class PushStreamContext implements StreamContext {
    private final Vault vault;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Monitor monitor;

    public PushStreamContext(Vault vault, OkHttpClient httpClient, ObjectMapper objectMapper, Monitor monitor) {
        this.vault = vault;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.monitor = monitor;
    }

    @Override
    public StreamSession createSession(String uri, String topicName, String secretName) {
        var accessToken = vault.resolveSecret(secretName);
        var destinationToken = readAccessToken(accessToken, secretName);
        var endpointUri = URI.create(uri);

        if ("ws".equalsIgnoreCase(endpointUri.getScheme())) {
            var session = new WsPushStreamSession(endpointUri, topicName, destinationToken, objectMapper, monitor);
            session.connect();
            return session;
        } else if ("https".equalsIgnoreCase(endpointUri.getScheme()) || "http".equalsIgnoreCase(endpointUri.getScheme())) {
            try {
                var endpointUrl = endpointUri.resolve(topicName).toURL();
                return new HttpStreamSession(endpointUrl, destinationToken, httpClient);
            } catch (MalformedURLException e) {
                throw new EdcException(e);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported scheme: " + endpointUri.getScheme());
        }
    }

    private String readAccessToken(@Nullable String accessToken, String destinationSecretName) {
        if (accessToken == null) {
            throw new EdcException("Access token not found in vault: " + destinationSecretName);
        }
        try {
            return objectMapper.readValue(accessToken, ProtocolsSecretToken.class).getToken();
        } catch (Exception e) {
            // NB: do not log the exception to avoid leaking credential information
            throw new EdcException(format("Error deserializing access token: %s [%s]", destinationSecretName, e.getClass().getName()));
        }
    }

}
