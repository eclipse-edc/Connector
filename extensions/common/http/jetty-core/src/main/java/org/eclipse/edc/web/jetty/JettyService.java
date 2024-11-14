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
 *       Fraunhofer Institute for Software and Systems Engineering - added method
 *       ZF Friedrichshafen AG - Set connector name
 *       Materna Information & Communications SE - disable Jetty send server version
 *
 */

package org.eclipse.edc.web.jetty;

import jakarta.servlet.Servlet;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
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
public class JettyService implements WebServer {

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
                if (!mapping.getPath().startsWith("/")) {
                    throw new IllegalArgumentException("A context path must start with /: " + mapping.getPath());
                }

                ServerConnector connector;
                if (Arrays.stream(server.getConnectors()).anyMatch(c -> ((ServerConnector) c).getPort() == mapping.getPort())) {
                    throw new IllegalArgumentException("A binding for port " + mapping.getPort() + " already exists");
                }

                if (keyStore != null) {
                    connector = httpsServerConnector(mapping.getPort());
                    monitor.debug("HTTPS context '" + mapping.getName() + "' listening on port " + mapping.getPort());
                } else {
                    connector = httpServerConnector();
                    monitor.debug("HTTP context '" + mapping.getName() + "' listening on port " + mapping.getPort());
                }

                connector.setName(mapping.getName());
                connector.setPort(mapping.getPort());

                configure(connector);
                server.addConnector(connector);

                var handler = createHandler(mapping);
                handlers.put(mapping.getPath(), handler);
            });
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

    @Override
    public void registerServlet(String contextName, Servlet servlet) {
        var servletHolder = new ServletHolder(Source.EMBEDDED);
        servletHolder.setName("EDC-" + contextName);
        servletHolder.setServlet(servlet);
        servletHolder.setInitOrder(1);

        var actualPath = configuration.getPortMappings().stream()
                .filter(pm -> Objects.equals(contextName, pm.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No PortMapping for contextName '" + contextName + "' found"))
                .getPath();

        var servletHandler = getOrCreate(actualPath).getServletHandler();
        servletHandler.addServletWithMapping(servletHolder, actualPath);

        var allPathSpec = actualPath.endsWith("/") ? "*" : "/*";
        servletHandler.addServletWithMapping(servletHolder, actualPath + allPathSpec);
    }

    /**
     * Allows adding a {@link PortMapping} that is not defined in the configuration. This can only
     * be done before the JettyService is started, i.e. before {@link #start()} is called.
     *
     * @param contextName name of the port mapping.
     * @param port        port of the port mapping.
     * @param path        path of the port mapping.
     */
    @Override
    public void addPortMapping(String contextName, int port, String path) {
        var portMapping = new PortMapping(contextName, port, path);
        if (server != null && (server.isStarted() || server.isStarting())) {
            return;
        }
        configuration.getPortMappings().add(portMapping);
    }

    public void addConnectorConfigurationCallback(Consumer<ServerConnector> callback) {
        connectorConfigurationCallbacks.add(callback);
    }

    @NotNull
    private ServletContextHandler createHandler(PortMapping mapping) {
        var handler = new ServletContextHandler(server, "/", NO_SESSIONS);
        handler.setVirtualHosts(new String[]{ "@" + mapping.getName() });
        return handler;
    }

    @NotNull
    private ServerConnector httpsServerConnector(int port) {
        var storePassword = configuration.getKeystorePassword();
        var managerPassword = configuration.getKeymanagerPassword();

        // for reference check:
        // https://medium.com/vividcode/enable-https-support-with-self-signed-certificate-for-embedded-jetty-9-d3a86f83e9d9
        var contextFactory = new SslContextFactory.Server();
        contextFactory.setKeyStore(keyStore);
        contextFactory.setKeyStorePassword(storePassword);
        contextFactory.setKeyManagerPassword(managerPassword);

        var httpsConfiguration = new HttpConfiguration();
        httpsConfiguration.setSecureScheme("https");
        httpsConfiguration.setSecurePort(port);
        httpsConfiguration.addCustomizer(new SecureRequestCustomizer());

        var httpConnectionFactory = new HttpConnectionFactory(httpsConfiguration);
        var sslConnectionFactory = new SslConnectionFactory(contextFactory, HttpVersion.HTTP_1_1.asString());
        return new ServerConnector(server, sslConnectionFactory, httpConnectionFactory);
    }

    @NotNull
    private ServerConnector httpServerConnector() {
        return new ServerConnector(server, httpConnectionFactory());
    }

    private void configure(ServerConnector connector) {
        connectorConfigurationCallbacks.forEach(c -> c.accept(connector));
    }

    @NotNull
    private HttpConnectionFactory httpConnectionFactory() {
        HttpConfiguration https = new HttpConfiguration();
        https.setSendServerVersion(false);
        return new HttpConnectionFactory(https);
    }

    private ServletContextHandler getOrCreate(String contextPath) {
        return handlers.computeIfAbsent(contextPath, k -> new ServletContextHandler(server, "/", NO_SESSIONS));
    }

}

