package org.eclipse.dataspaceconnector.transfer.demo.protocols.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.Subscription;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.DataMessage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.PubSubMessage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.PublishMessage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.SubscribeMessage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.stream.DemoTopicManager;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import static jakarta.websocket.CloseReason.CloseCodes.NORMAL_CLOSURE;
import static jakarta.websocket.CloseReason.CloseCodes.VIOLATED_POLICY;

/**
 * Handles the demo streaming protocol over web sockets.
 */
@ServerEndpoint(value = "/pubsub/")
public class PubSubServerEndpoint {
    private static final CloseReason UNSUBSCRIBE_REASON = new CloseReason(NORMAL_CLOSURE, "unsubscribe");
    private static final CloseReason NOT_AUTHORIZED_REASON = new CloseReason(VIOLATED_POLICY, "not authorized");

    private DemoTopicManager topicManager;
    private ObjectMapper objectMapper;
    private Monitor monitor;
    private Subscription subscription;

    public PubSubServerEndpoint(DemoTopicManager topicManager, ObjectMapper objectMapper, Monitor monitor) {
        this.topicManager = topicManager;
        this.objectMapper = objectMapper;
        this.monitor = monitor;
    }

    @OnMessage
    public void message(Session session, InputStream rawStream) throws IOException {
        var stream = new NonClosingInputStream(rawStream);
        var message = objectMapper.readValue(stream, PubSubMessage.class);

        switch (message.getProtocol()) {

            case SUBSCRIBE:
                var subscribe = (SubscribeMessage) message;
                var result = topicManager.subscribe(subscribe.getTopicName(), subscribe.getAccessToken(), payload -> {
                    try {
                        byte[] bytes = objectMapper.writeValueAsBytes(DataMessage.Builder.newInstance().topicName(subscribe.getTopicName()).payload(payload).build());
                        session.getBasicRemote().sendBinary(ByteBuffer.wrap(bytes));
                    } catch (IOException e) {
                        monitor.severe("Error connecting to websocket client", e);
                    }
                });
                if (!result.success()) {
                    session.close(NOT_AUTHORIZED_REASON);
                } else {
                    subscription = result.getSubscription();
                }
                break;
            case UNSUBSCRIBE:
                if (subscription != null) {
                    topicManager.unsubscribe(subscription);
                    session.close(UNSUBSCRIBE_REASON);
                }
                break;
            case CONNECT:
                // no-op
                break;
            case DISCONNECT:
                session.close(new CloseReason(NORMAL_CLOSURE, "disconnect"));
                break;
            case PUBLISH:
                var publish = (PublishMessage) message;
                var connectResult = topicManager.connect(publish.getTopicName(), publish.getAccessToken());
                if (!connectResult.success()) {
                    session.close(NOT_AUTHORIZED_REASON);
                } else {
                    connectResult.getConsumer().accept(publish.getPayload());
                }
                break;
        }
    }

    @OnError
    public void onWebSocketError(Throwable e) {
        if (e instanceof ClosedChannelException){
            // ignore
            return;
        }
        monitor.severe("Websocket error", e);
    }

}
