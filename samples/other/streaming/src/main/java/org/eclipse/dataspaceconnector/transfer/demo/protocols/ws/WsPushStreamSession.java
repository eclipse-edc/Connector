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

package org.eclipse.dataspaceconnector.transfer.demo.protocols.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.inline.StreamSession;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.PublishMessage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.UnSubscribeMessage;
import org.eclipse.jetty.util.component.LifeCycle;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

/**
 * A session for publishing data over a web socket to a destination endpoint.
 */
public class WsPushStreamSession implements StreamSession {
    private final URI uri;
    private final ObjectMapper objectMapper;
    private final Monitor monitor;
    private String topicName;
    private String topicToken;
    private Session session;
    private WebSocketContainer container;

    public WsPushStreamSession(URI uri, String topicName, String topicToken, ObjectMapper objectMapper, Monitor monitor) {
        this.uri = uri;
        this.topicName = topicName;
        this.topicToken = topicToken;
        this.objectMapper = objectMapper;
        this.monitor = monitor;
    }

    public void connect() {
        container = ContainerProvider.getWebSocketContainer();
        var endpoint = new PubSubClientEndpoint(objectMapper, monitor, m -> {
        });
        try {
            session = container.connectToServer(endpoint, uri);
        } catch (Exception e) {
            throw new EdcException("Error connecting to stream:" + e.getMessage());
        }
    }

    @Override
    public void publish(byte[] data) {
        try {
            var publishMessage = PublishMessage.Builder.newInstance()
                    .payload(data)
                    .topicName(topicName)
                    .accessToken(topicToken).build();

            session.getBasicRemote().sendBinary(ByteBuffer.wrap(objectMapper.writeValueAsBytes(publishMessage)));
        } catch (IOException e) {
            monitor.severe("Error pushing data stream", e);
        }
    }

    @Override
    public void close() {
        try {
            var disconnect = UnSubscribeMessage.Builder.newInstance().topicName(topicName).build();
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(objectMapper.writeValueAsBytes(disconnect)));
        } catch (IOException e) {
            throw new EdcException(e);
        } finally {
            topicName = null;
            topicToken = null; // NB: the access token must be removed
            session = null;
            if (container != null) {
                LifeCycle.stop(container);
                container = null;
            }
        }
    }

}
