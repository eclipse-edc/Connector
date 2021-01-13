package com.microsoft.dagx.web.rest;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.web.transport.JettyService;
import jakarta.ws.rs.core.Configuration;
import org.glassfish.jersey.server.ResourceConfig;

import java.util.HashSet;
import java.util.Set;

public class JerseyRestService {
    private JettyService jettyService;
    private Monitor monitor;

    public JerseyRestService(JettyService jettyService, Monitor monitor) {
        this.jettyService = jettyService;
        this.monitor = monitor;
    }

    public void start() {

        try {
            Configuration dooo;
            ResourceConfig l;

            Set<Object> controllers = new HashSet<>();
//            ExtendedConfig foo;
            // Create a Jersey JAX-RS Application
//            ResourceConfig resourceConfig = new ResourceConfig();
//            resourceConfig.registerInstances(controllers);
//
//            ServletContainer servletContainer = new ServletContainer(resourceConfig);
//            jettyService.registerServlet("/api/*", servletContainer);
            monitor.info("Registered Web API context");
        } catch (Exception e) {
            throw new DagxException(e);
        }
    }

}
