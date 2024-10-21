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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.web.jersey;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.jersey.feature.DynamicResourceFeature;
import org.eclipse.edc.web.jersey.mapper.EdcApiExceptionMapper;
import org.eclipse.edc.web.jersey.mapper.UnexpectedExceptionMapper;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toSet;
import static org.glassfish.jersey.server.ServerProperties.WADL_FEATURE_DISABLE;

public class JerseyRestService implements WebService {
    private static final String DEFAULT_CONTEXT_ALIAS = "default";

    private final WebServer webServer;
    private final TypeManager typeManager;
    private final Monitor monitor;

    private final Map<String, List<Object>> controllers = new HashMap<>();
    private final Map<String, Map<Class<?>, List<Object>>> dynamicResources = new HashMap<>();
    private final JerseyConfiguration configuration;
    private final List<Supplier<Object>> additionalInstances = new ArrayList<>();

    public JerseyRestService(WebServer webServer, TypeManager typeManager, JerseyConfiguration configuration, Monitor monitor) {
        this.webServer = webServer;
        this.typeManager = typeManager;
        this.configuration = configuration;
        this.monitor = monitor;
    }

    @Override
    public void registerResource(Object resource) {
        controllers.computeIfAbsent(DEFAULT_CONTEXT_ALIAS, s -> new ArrayList<>())
                .add(resource);
    }

    @Override
    public void registerResource(String contextAlias, Object resource) {
        controllers.computeIfAbsent(contextAlias, s -> new ArrayList<>())
                .add(resource);
    }

    @Override
    public void registerDynamicResource(String contextAlias, Class<?> target, Object resource) {
        dynamicResources
                .computeIfAbsent(contextAlias, s -> new HashMap<>())
                .computeIfAbsent(target, s -> new ArrayList<>())
                .add(resource);
    }

    void registerInstance(Supplier<Object> instance) {
        additionalInstances.add(instance);
    }

    public void start() {
        try {
            controllers.forEach(this::registerContext);
        } catch (Exception e) {
            throw new EdcException(e);
        }
    }

    private void registerContext(String contextAlias, List<Object> controllers) {
        var resourceConfig = new ResourceConfig();

        var dynamicResourcesForContext = dynamicResources.computeIfAbsent(contextAlias, s -> new HashMap<>());

        // Disable WADL as it is not used and emits a warning message about JAXB (which is also not used)
        resourceConfig.property(WADL_FEATURE_DISABLE, Boolean.TRUE);

        // Register controller (JAX-RS resources) with Jersey. Instances instead of classes are used so extensions may inject them with dependencies and manage their lifecycle.
        // In order to use instances with Jersey, the controller types must be registered along with an {@link AbstractBinder} that maps those types to the instances.
        resourceConfig.registerClasses(controllers.stream().map(Object::getClass).collect(toSet()));
        resourceConfig.registerInstances(new Binder(controllers));
        resourceConfig.registerInstances(new ObjectMapperProvider(typeManager.getMapper()));
        resourceConfig.registerInstances(new EdcApiExceptionMapper());
        resourceConfig.registerInstances(new UnexpectedExceptionMapper(monitor));
        resourceConfig.registerInstances(new DynamicResourceFeature(dynamicResourcesForContext));

        additionalInstances.forEach(supplier -> resourceConfig.registerInstances(supplier.get()));

        if (configuration.isCorsEnabled()) {
            resourceConfig.register(new CorsFilter(configuration));
        }
        resourceConfig.register(MultiPartFeature.class);

        var servletContainer = new ServletContainer(resourceConfig);
        webServer.registerServlet(contextAlias, servletContainer);

        monitor.info("Registered Web API context alias: " + contextAlias);
    }

    /**
     * Maps (JAX-RS resource) instances to types.
     */
    private static class Binder extends AbstractBinder {

        private final List<Object> controllers;

        Binder(List<Object> controllers) {
            this.controllers = controllers;
        }

        @Override
        protected void configure() {
            controllers.forEach(c -> bind(c).to((Class<? super Object>) c.getClass()));
        }
    }

}
