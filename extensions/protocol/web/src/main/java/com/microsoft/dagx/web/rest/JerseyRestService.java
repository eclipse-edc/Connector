/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.web.rest;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.web.transport.JettyService;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class JerseyRestService implements WebService {
    private static final String API_PATH = "/api/*";

    private JettyService jettyService;
    private TypeManager typeManager;
    private Monitor monitor;

    private Set<Object> controllers = new HashSet<>();

    public JerseyRestService(JettyService jettyService, TypeManager typeManager, Monitor monitor) {
        this.jettyService = jettyService;
        this.typeManager = typeManager;
        this.monitor = monitor;
    }

    @Override
    public void registerController(Object controller) {
        controllers.add(controller);
    }

    public void start() {
        try {
            // Create a Jersey JAX-RS Application
            ResourceConfig resourceConfig = new ResourceConfig();

            // Disable WADL as it is not used and emits a warning message about JAXB (which is also not used)
            resourceConfig.property(ServerProperties.WADL_FEATURE_DISABLE, Boolean.TRUE);

            // Register controller (JAX-RS resources) with Jersey. Instances instead of classes are used so extensions may inject them with dependencies and manage their lifecycle.
            // In order to use instances with Jersey, the controller types must be registered along with an {@link AbstractBinder} that maps those types to the instances.
            resourceConfig.registerClasses(controllers.stream().map(Object::getClass).collect(toSet()));
            resourceConfig.registerInstances(new Binder());

            resourceConfig.registerInstances(new TypeManagerContextResolver(typeManager));

            // Register the Jersey container with Jetty
            ServletContainer servletContainer = new ServletContainer(resourceConfig);
            jettyService.registerServlet(API_PATH, servletContainer);

            monitor.info("Registered Web API context at: " + API_PATH);
        } catch (Exception e) {
            throw new DagxException(e);
        }
    }

    /**
     * Maps (JAX-RS resource) instances to types.
     */
    private class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            //noinspection unchecked
            controllers.forEach(c -> bind(c).to((Class<? super Object>) c.getClass()));
        }
    }

}
