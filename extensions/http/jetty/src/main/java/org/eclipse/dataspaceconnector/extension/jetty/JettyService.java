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
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

/**
 * Provides HTTP(S) support using Jetty.
 */
public class JettyService {

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
        handlers.put("/", new ServletContextHandler(null, "/", NO_SESSIONS));
    }

    public void start() {
        try {
            var port = configuration.getHttpPort();
            server = new Server();

            if (keyStore != null) {
                server.addConnector(httpsServerConnector(port));
                monitor.info("HTTPS listening on " + port);
            } else {
                server.addConnector(httpServerConnector(port));
                monitor.info("HTTP listening on " + port);
            }

            server.setErrorHandler(new JettyErrorHandler());
            server.setHandler(new ContextHandlerCollection(handlers.values().toArray(ServletContextHandler[]::new)));

            server.start();
        } catch (Exception e) {
            throw new EdcException("Error starting Jetty service", e);
        }

    }

    public void shutdown() {
        try {
            if (server != null) {
                server.stop();
            }
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
        return sslConnector;
    }

    @NotNull
    private ServerConnector httpServerConnector(int port) {
        ServerConnector connector = new ServerConnector(server, httpConnectionFactory());
        connector.setPort(port);
        return connector;
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

