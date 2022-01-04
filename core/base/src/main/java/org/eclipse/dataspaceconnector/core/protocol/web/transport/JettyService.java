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

package org.eclipse.dataspaceconnector.core.protocol.web.transport;

import jakarta.servlet.Servlet;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
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

import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides HTTP(S) support using Jetty.
 */
public class JettyService {
    @EdcSetting
    private static final String HTTP_PORT = "web.http.port";

    private static final String LOG_ANNOUNCE = "org.eclipse.jetty.util.log.announce";
    private final JettyConfiguration configuration;
    private final Monitor monitor;
    private final KeyStore keyStore;
    private final Map<String, ServletContextHandler> handlers = new HashMap<>();
    private Server server;

    public JettyService(JettyConfiguration configuration, Monitor monitor) {
        this(configuration, null, monitor);
    }

    public JettyService(JettyConfiguration configuration, KeyStore keyStore, Monitor monitor) {
        this.configuration = configuration;
        this.keyStore = keyStore;
        this.monitor = monitor;
        System.setProperty(LOG_ANNOUNCE, "false");
        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        handler.setContextPath("/");
        handlers.put("/", handler);
    }

    public void start() {
        var port = configuration.getSetting(HTTP_PORT, "8181");

        try {
            if (keyStore != null) {
                server = new Server();
                var storePassword = configuration.getSetting("keystore.password", "password");
                var managerPassword = configuration.getSetting("keymanager.password", "password");

                SslContextFactory.Server contextFactory = new SslContextFactory.Server();
                contextFactory.setKeyStore(keyStore);
                contextFactory.setKeyStorePassword(storePassword);
                contextFactory.setKeyManagerPassword(managerPassword);

                HttpConfiguration https = new HttpConfiguration();
                SslConnectionFactory connectionFactory = new SslConnectionFactory(contextFactory, "http/1.1");
                ServerConnector sslConnector = new ServerConnector(server, connectionFactory, new HttpConnectionFactory(https));
                sslConnector.setPort(Integer.parseInt(port));
                server.setConnectors(new Connector[]{ sslConnector });
                monitor.info("HTTPS listening on " + port);
            } else {
                server = new Server(Integer.parseInt(port));
                ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration()));
                connector.setPort(Integer.parseInt(port));
                server.setConnectors(new Connector[]{ connector });
                monitor.info("HTTP listening on " + port);
            }

            server.setErrorHandler(new JettyErrorHandler());
            ContextHandlerCollection contexts = new ContextHandlerCollection();
            contexts.setHandlers(handlers.values().toArray(new Handler[0]));
            server.setHandler(contexts);

            server.start();
        } catch (Exception e) {
            throw new EdcException("Error starting Jetty service", e);
        }

    }

    public void shutdown() {
        if (server == null) {
            return;
        }
        try {
            server.stop();
        } catch (Exception e) {
            throw new EdcException("Error shutting down Jetty service", e);
        }
    }

    public void registerServlet(String contextPath, String path, Servlet servlet) {
        ServletHolder servletHolder = new ServletHolder(Source.EMBEDDED);
        servletHolder.setName("EDC");
        servletHolder.setServlet(servlet);
        servletHolder.setInitOrder(1);

        var handler = getOrCreate(contextPath);

        handler.getServletHandler().addServletWithMapping(servletHolder, path);
    }

    public ServletContextHandler getHandler(String path) {
        return handlers.get(path);
    }

    public void registerHandler(ServletContextHandler handler) {
        handlers.put(handler.getContextPath(), handler);
    }

    private ServletContextHandler getOrCreate(String contextPath) {
        return handlers.computeIfAbsent(contextPath, k -> {
            ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
            handler.setContextPath(contextPath);
            return handler;
        });
    }

}

