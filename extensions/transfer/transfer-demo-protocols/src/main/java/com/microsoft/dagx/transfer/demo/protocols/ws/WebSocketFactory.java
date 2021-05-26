package com.microsoft.dagx.transfer.demo.protocols.ws;

import com.microsoft.dagx.web.transport.JettyService;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Binds a web socket endpoint to the runtime Jetty service.
 */
public class WebSocketFactory {
    private static final int MESSAGE_BUFFER_SIZE = 65535;

    public WebSocketFactory() {
    }

    public void publishEndpoint(Class<?> endpointClass, Supplier<Object> endpointSupplier, JettyService jettyService) {
        var handler = jettyService.getHandler("/");
        JakartaWebSocketServletContainerInitializer.configure(handler, (servletContext, wsContainer) -> {
            wsContainer.setDefaultMaxTextMessageBufferSize(MESSAGE_BUFFER_SIZE);
            var config = endpointFactory(endpointClass, endpointSupplier);
            wsContainer.addEndpoint(config);
        });
    }

    @NotNull
    private ServerEndpointConfig endpointFactory(Class<?> endpointClass, Supplier<Object>  endpointSupplier) {
        var endpointAnnotation = endpointClass.getAnnotation(ServerEndpoint.class);
        return ServerEndpointConfig.Builder.create(endpointClass, endpointAnnotation.value())
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> clazz) {
                        return clazz.cast(endpointSupplier.get());
                    }
                }).build();
    }
}
