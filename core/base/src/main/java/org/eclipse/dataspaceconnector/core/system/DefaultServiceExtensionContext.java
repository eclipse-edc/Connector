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

package org.eclipse.dataspaceconnector.core.system;

import org.eclipse.dataspaceconnector.core.BaseExtension;
import org.eclipse.dataspaceconnector.core.CoreExtension;
import org.eclipse.dataspaceconnector.core.util.TopologicalSort;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Base service extension context.
 * <p>Prior to using, {@link #initialize()} must be called.</p>
 */
public class DefaultServiceExtensionContext implements ServiceExtensionContext {
    private final Monitor monitor;
    private final TypeManager typeManager;

    private final Map<Class<?>, Object> services = new HashMap<>();
    private final ServiceLocator serviceLocator;
    private List<ConfigurationExtension> configurationExtensions;
    private String connectorId;

    public DefaultServiceExtensionContext(TypeManager typeManager, Monitor monitor) {
        this(typeManager, monitor, new ServiceLocatorImpl());
    }

    public DefaultServiceExtensionContext(TypeManager typeManager, Monitor monitor, ServiceLocator serviceLocator) {
        this.typeManager = typeManager;
        this.monitor = monitor;
        this.serviceLocator = serviceLocator;
        // register as services
        registerService(TypeManager.class, typeManager);
        registerService(Monitor.class, monitor);
    }

    @Override
    public String getConnectorId() {
        return connectorId;
    }

    @Override
    public Monitor getMonitor() {
        return monitor;
    }

    @Override
    public TypeManager getTypeManager() {
        return typeManager;
    }

    /**
     * Attempts to resolve the setting by delegating to configuration extensions, VM properties, and then env variables, in that order; otherwise
     * the default value is returned.
     */
    @Override
    public String getSetting(String key, String defaultValue) {
        String value;
        for (ConfigurationExtension extension : configurationExtensions) {
            value = extension.getSetting(key);
            if (value != null) {
                return value;
            }
        }
        value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public <T> boolean hasService(Class<T> type) {
        return services.containsKey(type);
    }

    @Override
    public <T> T getService(Class<T> type) {
        T service = (T) services.get(type);
        if (service == null) {
            throw new EdcException("Service not found: " + type.getName());
        }
        return service;
    }

    @Override
    public <T> T getService(Class<T> type, boolean isOptional) {
        if (!isOptional) {
            return getService(type);
        }
        return (T) services.get(type);
    }

    @Override
    public <T> void registerService(Class<T> type, T service) {
        if (hasService(type)) {
            monitor.warning("A service of the type " + type.getCanonicalName() + " was already registered and has now been replaced");
        }
        services.put(type, service);
    }

    @Override
    public List<ServiceExtension> loadServiceExtensions() {
        List<ServiceExtension> serviceExtensions = loadExtensions(ServiceExtension.class, true);

        //the first sort is only to verify that there are no "upward" dependencies from PRIMORDIAL -> DEFAULT
        sortExtensions(serviceExtensions);

        return Collections.unmodifiableList(serviceExtensions);
    }

    @Override
    public <T> List<T> loadExtensions(Class<T> type, boolean required) {
        return serviceLocator.loadImplementors(type, required);
    }

    @Override
    public <T> T loadSingletonExtension(Class<T> type, boolean required) {
        return serviceLocator.loadSingletonImplementor(type, required);
    }

    @Override
    public void initialize() {
        configurationExtensions = loadExtensions(ConfigurationExtension.class, false);
        configurationExtensions.forEach(ext -> {
            ext.initialize(monitor);
            monitor.info("Initialized " + ext.name());
        });
        connectorId = getSetting("edc.connector.name", "edc-" + UUID.randomUUID());
    }

    private void sortExtensions(List<ServiceExtension> loadedExtensions) {
        Map<String, List<ServiceExtension>> dependencyMap = new HashMap<>();
        var allProvided = loadedExtensions.stream().flatMap(se -> getProvidedFeatures(se).stream()).collect(Collectors.toSet());

        addDefaultExtensions(loadedExtensions);

        // add all provided features to the dependency map
        loadedExtensions.forEach(ext -> getProvidedFeatures(ext).forEach(feature -> dependencyMap.computeIfAbsent(feature, k -> new ArrayList<>()).add(ext)));

        TopologicalSort<ServiceExtension> sort = new TopologicalSort<>();

        // check if all required dependencies are satisfied, throw exception otherwise
        loadedExtensions.forEach(ext -> getRequiredFeatures(ext, allProvided).forEach(feature -> {
            List<ServiceExtension> dependencies = dependencyMap.get(feature);
            if (dependencies == null) {
                throw new EdcException(format("Extension feature \"%s\" required by %s was not found", feature, ext.getClass().getName()));
            } else {
                dependencies.forEach(dependency -> sort.addDependency(ext, dependency));
            }
        }));
        sort.sort(loadedExtensions);
    }

    /**
     * Handles core-, transfer- and contract-extensions and inserts them at the beginning of the list so that
     * explicit @Requires annotations are not necessary
     */
    private void addDefaultExtensions(List<ServiceExtension> loadedExtensions) {
        var baseDependencies = loadedExtensions.stream().filter(e -> e.getClass().getAnnotation(BaseExtension.class) != null).collect(Collectors.toList());
        if (baseDependencies.isEmpty()) {
            throw new EdcException("No base dependencies were found on the classpath. Please add the \"core:base\" module to your classpath!");
        }
        var coreDependencies = loadedExtensions.stream().filter(e -> e.getClass().getAnnotation(CoreExtension.class) != null).collect(Collectors.toList());

        //make sure the core- and transfer-dependencies are ALWAYS ordered first
        loadedExtensions.removeAll(baseDependencies);
        loadedExtensions.removeAll(coreDependencies);
        coreDependencies.forEach(se -> loadedExtensions.add(0, se));
        baseDependencies.forEach(se -> loadedExtensions.add(0, se));
    }

    /**
     * Obtains all features a specific extension provides as strings
     */
    private Set<String> getRequiredFeatures(ServiceExtension ext, Set<String> allFeatures) {
        // initialize with legacy list
        var allRequired = new HashSet<>(ext.requires());

        var requiresAnnotation = ext.getClass().getAnnotation(Requires.class);
        if (requiresAnnotation != null) {

            // all feature classes NOT annotate with @Feature will be listed under the "false" key
            var stream = Arrays.stream(requiresAnnotation.value());
            Map<Boolean, List<Class<?>>> requiredFeatureIsAnnotated = stream.collect(Collectors.partitioningBy(x -> x.getAnnotation(Feature.class) != null));

            if (!requiredFeatureIsAnnotated.get(false).isEmpty()) {
                throw new EdcException("One or more @Require'd features are not annotated with \"@Feature\": " + requiredFeatureIsAnnotated.get(false).stream().map(Class::getName).collect(Collectors.joining(",")));
            }

            var annotatedProvides = requiredFeatureIsAnnotated.get(true).stream().map(this::getFeatureValue).flatMap(feature -> expand(feature, allFeatures).stream()).collect(Collectors.toList());
            allRequired.addAll(annotatedProvides);
        }

        return allRequired;
    }

    /**
     * expands parent features (e.g. "some:feature" into child features, if they are proviced, e.g. "some:feature:subfeature"
     *
     * @param requiredFeature The potential parent feature. Could also be the child feature.
     * @param allFeatures     All available features provided by the entirety of all extensions.
     * @return If the given required feature is a parent feature, all sub-features that are in the same namespace are returned. Else, a singleton Set of the required feature itself is returned.
     */
    private Set<String> expand(String requiredFeature, Set<String> allFeatures) {
        return allFeatures.stream().filter(feature -> feature.startsWith(requiredFeature)).collect(Collectors.toSet());
    }


    /**
     * Obtains all features a specific extension requires as strings
     */
    private Set<String> getProvidedFeatures(ServiceExtension ext) {
        var allProvides = new HashSet<>(ext.provides());

        var providesAnnotation = ext.getClass().getAnnotation(Provides.class);
        if (providesAnnotation != null) {

            // all feature classes NOT annotate with @Feature will be listed under the "false" key
            var stream = Arrays.stream(providesAnnotation.value());
            Map<Boolean, List<Class<?>>> providedFeatureIsAnnotated = stream.collect(Collectors.partitioningBy(x -> x.getAnnotation(Feature.class) != null));

            if (!providedFeatureIsAnnotated.get(false).isEmpty()) {
                throw new EdcException("One or more @Provide'd features are not annotated with \"@Feature\": " + providedFeatureIsAnnotated.get(false).stream().map(Class::getName).collect(Collectors.joining(",")));
            }

            var annotatedProvides = providedFeatureIsAnnotated.get(true).stream().map(this::getFeatureValue).collect(Collectors.toSet());
            allProvides.addAll(annotatedProvides);
        }
        return allProvides;
    }

    private String getFeatureValue(Class<?> featureClass) {
        return featureClass.getAnnotation(Feature.class).value();
    }


}
