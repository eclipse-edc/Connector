package com.microsoft.dagx.transfer.demo.protocols.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.PublishMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.UnSubscribeMessage;
import com.microsoft.dagx.transfer.demo.protocols.stream.StreamSession;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.eclipse.jetty.util.component.LifeCycle;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

/**
 * A session for publishing data over a web socket to a destination endpoint.
 */
public class WsPushStreamSession implements StreamSession {
    private ObjectMapper objectMapper;
    private Monitor monitor;

    private final URI uri;
    private String destinationName;
    private String destinationToken;
    private Session session;
    private WebSocketContainer container;

    public WsPushStreamSession(URI uri, String destinationName, String destinationToken, ObjectMapper objectMapper, Monitor monitor) {
        this.uri = uri;
        this.destinationName = destinationName;
        this.destinationToken = destinationToken;
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
            throw new DagxException("Error connecting to stream:" + e.getMessage());
        }
    }

    @Override
    public void publish(byte[] data) {
        try {
            var publishMessage = PublishMessage.Builder.newInstance()
                    .payload(data)
                    .destinationName(destinationName)
                    .accessToken(destinationToken).build();

            session.getBasicRemote().sendBinary(ByteBuffer.wrap(objectMapper.writeValueAsBytes(publishMessage)));
        } catch (IOException e) {
            monitor.severe("Error pushing data stream", e);
        }
    }

    @Override
    public void close() {
        try {
            var disconnect = UnSubscribeMessage.Builder.newInstance().destinationName(destinationName).build();
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(objectMapper.writeValueAsBytes(disconnect)));
        } catch (IOException e) {
            throw new DagxException(e);
        } finally {
            destinationName = null;
            destinationToken = null; // NB: the access token must be removed
            session = null;
            if (container != null) {
                LifeCycle.stop(container);
                container = null;
            }
        }
    }

}
