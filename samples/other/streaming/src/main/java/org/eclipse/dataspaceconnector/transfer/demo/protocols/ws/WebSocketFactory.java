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

import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import org.eclipse.dataspaceconnector.extension.jetty.JettyService;
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
    private ServerEndpointConfig endpointFactory(Class<?> endpointClass, Supplier<Object> endpointSupplier) {
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
