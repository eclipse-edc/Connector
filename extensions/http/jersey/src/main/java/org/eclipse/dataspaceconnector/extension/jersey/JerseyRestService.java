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

package org.eclipse.dataspaceconnector.extension.jersey;

import org.eclipse.dataspaceconnector.extension.jetty.JettyService;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.glassfish.jersey.server.ServerProperties.WADL_FEATURE_DISABLE;

public class JerseyRestService implements WebService {
    private static final String API_PATH = "/api/*";

    private final JettyService jettyService;
    private final TypeManager typeManager;
    private final Monitor monitor;

    private final Set<Object> controllers = new HashSet<>();
    private final CorsFilterConfiguration corsConfiguration;

    public JerseyRestService(JettyService jettyService, TypeManager typeManager, CorsFilterConfiguration corsConfiguration, Monitor monitor) {
        this.jettyService = jettyService;
        this.typeManager = typeManager;
        this.corsConfiguration = corsConfiguration;
        this.monitor = monitor;
    }

    @Override
    public void registerController(Object controller) {
        controllers.add(controller);
    }

    public void start() {
        try {
            var resourceConfig = new ResourceConfig();

            // Disable WADL as it is not used and emits a warning message about JAXB (which is also not used)
            resourceConfig.property(WADL_FEATURE_DISABLE, Boolean.TRUE);

            // Register controller (JAX-RS resources) with Jersey. Instances instead of classes are used so extensions may inject them with dependencies and manage their lifecycle.
            // In order to use instances with Jersey, the controller types must be registered along with an {@link AbstractBinder} that maps those types to the instances.
            resourceConfig.registerClasses(controllers.stream().map(Object::getClass).collect(toSet()));
            resourceConfig.registerInstances(new Binder());
            resourceConfig.registerInstances(new TypeManagerContextResolver(typeManager));

            if (corsConfiguration.isCorsEnabled()) {
                resourceConfig.register(new CorsFilter(corsConfiguration));
            }
            resourceConfig.register(MultiPartFeature.class);

            var servletContainer = new ServletContainer(resourceConfig);
            jettyService.registerServlet("/", API_PATH, servletContainer);

            monitor.info("Registered Web API context at: " + API_PATH);
        } catch (Exception e) {
            throw new EdcException(e);
        }
    }

    /**
     * Maps (JAX-RS resource) instances to types.
     */
    private class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            controllers.forEach(c -> bind(c).to((Class<? super Object>) c.getClass()));
        }
    }

}
