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

package org.eclipse.dataspaceconnector.extension.jetty;

import jakarta.servlet.Servlet;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.Source;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jetbrains.annotations.NotNull;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

/**
 * Provides HTTP(S) support using Jetty.
 */
public class JettyService {

    public static final String DEFAULT_ROOT_PATH = "/api";
    private static final String LOG_ANNOUNCE = "org.eclipse.jetty.util.log.announce";
    private final JettyConfiguration configuration;
    private final Monitor monitor;
    private final KeyStore keyStore;
    private final Map<String, ServletContextHandler> handlers = new HashMap<>();
    private final List<Consumer<ServerConnector>> connectorConfigurationCallbacks = new ArrayList<>();
    private Server server;

    public JettyService(JettyConfiguration configuration, Monitor monitor) {
        this(configuration, null, monitor);
    }

    public JettyService(JettyConfiguration configuration, KeyStore keyStore, Monitor monitor) {
        this.configuration = configuration;
        this.keyStore = keyStore;
        this.monitor = monitor;
        System.setProperty(LOG_ANNOUNCE, "false");
        // for websocket endpoints
        handlers.put("/", new ServletContextHandler(null, "/", NO_SESSIONS));
    }

    public void start() {
        try {
            server = new Server();
            //create a connector for every port mapping
            configuration.getPortMappings().forEach(mapping -> {

                ServerConnector connector;
                if (Arrays.stream(server.getConnectors()).anyMatch(c -> ((ServerConnector) c).getPort() == mapping.getPort())) {
                    throw new IllegalArgumentException("A binding for port " + mapping.getPort() + " already exists");
                }
                if (keyStore != null) {
                    connector = httpsServerConnector(mapping.getPort());
                    monitor.info("HTTPS context '" + mapping.getName() + "' listening on port " + mapping.getPort());
                } else {
                    connector = httpServerConnector(mapping.getPort());
                    monitor.info("HTTP context '" + mapping.getName() + "' listening on port " + mapping.getPort());
                }
                connector.setName(mapping.getName());
                server.addConnector(connector);

                ServletContextHandler handler = createHandler(mapping);
                handlers.put(mapping.getPath(), handler);
            });
            server.setErrorHandler(new JettyErrorHandler());
            server.setHandler(new ContextHandlerCollection(handlers.values().toArray(ServletContextHandler[]::new)));
            server.start();
            monitor.debug("Port mappings: " + configuration.getPortMappings().stream().map(PortMapping::toString).collect(Collectors.joining(", ")));
        } catch (Exception e) {
            throw new EdcException("Error starting Jetty service", e);
        }

    }

    public void shutdown() {
        try {
            if (server != null) {
                server.stop();
                server.join(); //wait for all threads to wind down
            }
        } catch (Exception e) {
            throw new EdcException("Error shutting down Jetty service", e);
        }
    }

    public void registerServlet(String contextName, Servlet servlet) {
        ServletHolder servletHolder = new ServletHolder(Source.EMBEDDED);
        servletHolder.setName("EDC-" + contextName); //must be unique
        servletHolder.setServlet(servlet);
        servletHolder.setInitOrder(1);

        var actualPath = configuration.getPortMappings().stream()
                .filter(pm -> Objects.equals(contextName, pm.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No PortMapping for contextName '" + contextName + "' found"))
                .getPath();

        var handler = getOrCreate(actualPath);
        handler.getServletHandler().addServletWithMapping(servletHolder, "/*");
    }

    public ServletContextHandler getHandler(String path) {
        return handlers.get(path);
    }

    @NotNull
    private ServletContextHandler createHandler(PortMapping mapping) {
        var handler = new ServletContextHandler(null, mapping.getPath(), NO_SESSIONS);
        handler.setVirtualHosts(new String[]{ "@" + mapping.getName() });
        return handler;
    }

    @NotNull
    private ServerConnector httpsServerConnector(int port) {
        var storePassword = configuration.getKeystorePassword();
        var managerPassword = configuration.getKeymanagerPassword();

        var contextFactory = new SslContextFactory.Server();
        contextFactory.setKeyStore(keyStore);
        contextFactory.setKeyStorePassword(storePassword);
        contextFactory.setKeyManagerPassword(managerPassword);

        var sslConnectionFactory = new SslConnectionFactory(contextFactory, "http/1.1");
        var sslConnector = new ServerConnector(server, httpConnectionFactory(), sslConnectionFactory);
        sslConnector.setPort(port);
        configure(sslConnector);
        return sslConnector;
    }

    @NotNull
    private ServerConnector httpServerConnector(int port) {
        ServerConnector connector = new ServerConnector(server, httpConnectionFactory());
        connector.setPort(port);
        configure(connector);
        return connector;
    }

    private void configure(ServerConnector connector) {
        connectorConfigurationCallbacks.forEach(c -> c.accept(connector));
    }

    public void addConnectorConfigurationCallback(Consumer<ServerConnector> callback) {
        connectorConfigurationCallbacks.add(callback);
    }

    @NotNull
    private HttpConnectionFactory httpConnectionFactory() {
        HttpConfiguration https = new HttpConfiguration();
        return new HttpConnectionFactory(https);
    }

    private ServletContextHandler getOrCreate(String contextPath) {
        return handlers.computeIfAbsent(contextPath, k -> {
            ServletContextHandler handler = new ServletContextHandler(NO_SESSIONS);
            handler.setContextPath(contextPath);
            return handler;
        });
    }

}

