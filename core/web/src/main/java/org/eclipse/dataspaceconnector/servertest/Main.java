package org.eclipse.dataspaceconnector.servertest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

public class Main {
    public static void main(String[] args) throws Exception {
        Server server = new Server();
        // External
        final ExternalResource externalResource = new ExternalResource();
        final ResourceConfig externalResourceConfig = new ResourceConfig();
        externalResourceConfig.property(ServerProperties.WADL_FEATURE_DISABLE, Boolean.TRUE);
        externalResourceConfig.registerInstances(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(externalResource).to(ExternalResource.class);
            }
        });
        externalResourceConfig.register(ExternalResource.class);
        final ServletContextHandler externalContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        final ServletHolder externalServletHolder = new ServletHolder(new ServletContainer(externalResourceConfig));
        externalContext.addServlet(externalServletHolder, "/*");
        externalContext.setVirtualHosts(new String[]{"@External"});
        final ServerConnector externalConnector = new ServerConnector(server);
        externalConnector.setPort(8080);
        externalConnector.setName("External");
        server.addConnector(externalConnector);


        // Internal Context
        final InternalResource internalResource = new InternalResource();
        final ResourceConfig internalResourceConfig = new ResourceConfig();
        internalResourceConfig.property(ServerProperties.WADL_FEATURE_DISABLE, Boolean.TRUE);
        internalResourceConfig.registerInstances(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(internalResource).to(InternalResource.class);
            }
        });
        internalResourceConfig.register(InternalResource.class);
        final ServletContextHandler internalContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        final ServletHolder internalServletHolder = new ServletHolder(new ServletContainer(internalResourceConfig));
        internalContext.addServlet(internalServletHolder, "/*");
        internalContext.setVirtualHosts(new String[]{"@Internal"});
        final ServerConnector internalConnector = new ServerConnector(server);
        internalConnector.setPort(8081);
        internalConnector.setName("Internal");
        server.addConnector(internalConnector);
        server.setHandler(new HandlerList(externalContext, internalContext));
        server.start();
    }
    @Path("/")
    public static class InternalResource {
        public InternalResource() {}
        @GET
        public String index() {
            return "internal";
        }
    }
    @Path("/")
    public static class ExternalResource {
        public ExternalResource() {}
        @GET
        public String index() {
            return "external";
        }
    }
}