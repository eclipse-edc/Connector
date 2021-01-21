package com.microsoft.dagx.web.transport;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
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
import org.glassfish.jersey.servlet.ServletContainer;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides HTTP(S) support using Jetty.
 */
public class JettyService {
    private static final String LOG_ANNOUNCE = "org.eclipse.jetty.util.log.announce";
    private final JettyConfiguration configuration;
    private KeyStore keyStore;
    private final Monitor monitor;

    private Server server;
    private List<Handler> handlers = new ArrayList<>();

    public JettyService(JettyConfiguration configuration, Monitor monitor) {
        this.configuration = configuration;
        this.monitor = monitor;
        System.setProperty(LOG_ANNOUNCE, "false");
    }

    public JettyService(JettyConfiguration configuration, KeyStore keyStore, Monitor monitor) {
        this.configuration = configuration;
        this.keyStore = keyStore;
        this.monitor = monitor;
        System.setProperty(LOG_ANNOUNCE, "false");
    }

    public void start() {
        var port = configuration.getSetting("web.http.port", 8080);

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
                sslConnector.setPort(port);
                server.setConnectors(new Connector[]{sslConnector});
                monitor.info("HTTPS listening on " + port);
            } else {
                server = new Server(port);
                ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration()));
                connector.setPort(port);
                server.setConnectors(new Connector[]{connector});
                monitor.info("HTTP listening on " + port);
            }

            server.setErrorHandler(new JettyErrorHandler());
            ContextHandlerCollection contexts = new ContextHandlerCollection();
            contexts.setHandlers(handlers.toArray(new Handler[0]));
            server.setHandler(contexts);

            server.start();
        } catch (Exception e) {
            throw new DagxException("Error starting Jetty service", e);
        }

    }

    public void shutdown() {
        if (server == null) {
            return;
        }
        try {
            server.stop();
        } catch (Exception e) {
            throw new DagxException("Error shutting down Jetty service", e);
        }
    }

    public void registerServlet(String path, ServletContainer servletContainer) {
        ServletHolder servletHolder = new ServletHolder(Source.EMBEDDED);
        servletHolder.setName("DA-GX");
        servletHolder.setServlet(servletContainer);
        servletHolder.setInitOrder(1);

        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        handler.setContextPath("/");

        handlers.add(handler);

        handler.getServletHandler().addServletWithMapping(servletHolder, path);
    }
}

