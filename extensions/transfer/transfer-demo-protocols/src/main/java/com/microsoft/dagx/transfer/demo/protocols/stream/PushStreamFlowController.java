package com.microsoft.dagx.transfer.demo.protocols.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.flow.DataFlowController;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.DestinationSecretToken;
import com.microsoft.dagx.transfer.demo.protocols.spi.DemoProtocols;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.PublishMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.UnSubscribeMessage;
import com.microsoft.dagx.transfer.demo.protocols.ws.PubSubClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import org.eclipse.jetty.util.component.LifeCycle;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.ByteBuffer;

/**
 * Implements push-style streaming. The client runtime provisions a destination which the provider runtime publishes to.
 */
public class PushStreamFlowController implements DataFlowController {
    private Vault vault;
    private ObjectMapper objectMapper;
    private Monitor monitor;

    public PushStreamFlowController(Vault vault, ObjectMapper objectMapper, Monitor monitor) {
        this.vault = vault;
        this.objectMapper = objectMapper;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return DemoProtocols.PUSH_STREAM.equals(dataRequest.getDestinationType());
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiateFlow(DataRequest dataRequest) {
        sendMessage(dataRequest);
        return DataFlowInitiateResponse.OK;
    }

    void sendMessage(DataRequest dataRequest) {
        var dataAddress = dataRequest.getDataDestination();

        var uriProperty = dataAddress.getProperty(DemoProtocols.ENDPOINT_ADDRESS);
        // TODO handle nulls
        var uri = URI.create(uriProperty);
        var destinationName = dataAddress.getProperty(DemoProtocols.DESTINATION_NAME);
        var destinationSecretName = dataRequest.getDataDestination().getKeyName();
        var accessToken = vault.resolveSecret(destinationSecretName);
        try {
            var destinationToken = objectMapper.readValue(accessToken, DestinationSecretToken.class);
            var container = ContainerProvider.getWebSocketContainer();

            var endpoint = new PubSubClientEndpoint(objectMapper, monitor, m -> {
            });

            try (Session session = container.connectToServer(endpoint, uri)) {

                var destination = PublishMessage.Builder.newInstance()
                        .payload("test message".getBytes())
                        .destinationName(destinationName)
                        .accessToken(destinationToken.getToken()).build();

                session.getBasicRemote().sendBinary(ByteBuffer.wrap(objectMapper.writeValueAsBytes(destination)));

                var disconnect = UnSubscribeMessage.Builder.newInstance().destinationName(destinationName).build();
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(objectMapper.writeValueAsBytes(disconnect)));
            } finally {
                LifeCycle.stop(container);
            }
        } catch (Exception e) {
            monitor.severe("Error pushing data stream", e);
        }
    }
}
