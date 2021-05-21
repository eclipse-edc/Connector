package com.microsoft.dagx.transfer.demo.protocols.ws;

import com.microsoft.dagx.web.transport.JettyService;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.jetbrains.annotations.NotNull;

/**
 * Binds a web socket endpoint to the runtime Jetty service.
 */
public class WebSocketFactory {
    private static final int MESSAGE_BUFFER_SIZE = 65535;

    public WebSocketFactory() {
    }

    public void publishEndpoint(Object endpoint, JettyService jettyService) {
        var handler = jettyService.getHandler("/");
        JakartaWebSocketServletContainerInitializer.configure(handler, (servletContext, wsContainer) -> {
            wsContainer.setDefaultMaxTextMessageBufferSize(MESSAGE_BUFFER_SIZE);
            var config = endpointFactory(endpoint);
            wsContainer.addEndpoint(config);
        });
    }

    @NotNull
    private ServerEndpointConfig endpointFactory(Object instance) {
        var endpointAnnotation = instance.getClass().getAnnotation(ServerEndpoint.class);
        return ServerEndpointConfig.Builder.create(instance.getClass(), endpointAnnotation.value())
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> clazz) {
                        return clazz.cast(instance);
                    }
                }).build();
    }
}
