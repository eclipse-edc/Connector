package com.microsoft.dagx.transfer.demo.protocols.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.DataMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.PubSubMessage;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

/**
 * Implements a web socket client endpoint that receives data published to a topic that the current runtime is subscribed to.
 */
@ClientEndpoint
public class PubSubClientEndpoint {
    private ObjectMapper objectMapper;
    private Monitor monitor;
    private PubSubConsumer consumer;

    public PubSubClientEndpoint(ObjectMapper objectMapper, Monitor monitor, PubSubConsumer consumer) {
        this.objectMapper = objectMapper;
        this.monitor = monitor;
        this.consumer = consumer;
    }

    @OnMessage
    public void message(Session session, byte[] payload) throws IOException {
        PubSubMessage message = objectMapper.readValue(payload, PubSubMessage.class);
        if (message.getProtocol() == PubSubMessage.Protocol.DATA) {
            consumer.accept((DataMessage) message);
        } else {
            monitor.severe("Unexpected message type: " + message.getProtocol());
        }
    }

    @OnClose
    public void close(CloseReason reason) {
        consumer.closed();
    }

    @OnError
    public void error(Throwable e) {
        if (e instanceof ClosedChannelException){
            // ignore
            return;
        }
        monitor.severe("Websocket error", e);
    }

}
