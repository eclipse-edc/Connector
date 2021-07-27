/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.system;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ConfigurationExtension;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.util.TopologicalSort;

import java.util.*;
import java.util.stream.Collectors;

import static com.microsoft.dagx.spi.system.ServiceExtension.LoadPhase.DEFAULT;
import static com.microsoft.dagx.spi.system.ServiceExtension.LoadPhase.PRIMORDIAL;
import static java.lang.String.format;
import static java.util.stream.Collectors.toCollection;

/**
 * Base service extension context.
 * <p>
 * Prior to using, {@link #initialize()} must be called.
 */
public class DefaultServiceExtensionContext implements ServiceExtensionContext {
    private final Monitor monitor;
    private final TypeManager typeManager;

    private final Map<Class<?>, Object> services = new HashMap<>();
    private final ServiceLocator serviceLocator;
    private List<ConfigurationExtension> configurationExtensions;

    public DefaultServiceExtensionContext(TypeManager typeManager, Monitor monitor) {
        this(typeManager, monitor, new ServiceLocatorImpl());
    }

    public DefaultServiceExtensionContext(TypeManager typeManager, Monitor monitor, ServiceLocator serviceLocator) {
        this.typeManager = typeManager;
        this.monitor = monitor;
        this.serviceLocator = serviceLocator;
        // register as services
        services.put(TypeManager.class, typeManager);
        services.put(Monitor.class, monitor);
    }

    public void initialize() {
        configurationExtensions = loadExtensions(ConfigurationExtension.class, false);
        configurationExtensions.forEach(ext -> ext.initialize(monitor));
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
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> type) {
        T service = (T) services.get(type);
        if (service == null) {
            throw new DagxException("Service not found: " + type.getName());
        }
        return service;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> type, boolean isOptional) {
        if (!isOptional) {
            return getService(type);
        }
        return (T) services.get(type);
    }

    @Override
    public <T> void registerService(Class<T> type, T service) {
        services.put(type, service);
    }

    @Override
    public List<ServiceExtension> loadServiceExtensions() {
        List<ServiceExtension> serviceExtensions = loadExtensions(ServiceExtension.class, true);
        List<ServiceExtension> primordialExtensions = serviceExtensions.stream().filter(ext -> ext.phase() == PRIMORDIAL).collect(toCollection(ArrayList::new));
        List<ServiceExtension> defaultExtensions = serviceExtensions.stream().filter(ext -> ext.phase() == DEFAULT).collect(toCollection(ArrayList::new));

        //the first sort is only to verify that there are no "upward" dependencies from PRIMORDIAL -> DEFAULT
        sortExtensions(primordialExtensions, Collections.emptySet());
        sortExtensions(defaultExtensions, primordialExtensions.stream().flatMap(e -> e.provides().stream()).collect(Collectors.toSet()));

        List<ServiceExtension> totalOrdered = new ArrayList<>(primordialExtensions);
        totalOrdered.addAll(defaultExtensions);
        return totalOrdered;
    }

    @Override
    public <T> List<T> loadExtensions(Class<T> type, boolean required) {
        return serviceLocator.loadImplementors(type, required);
    }

    @Override
    public <T> T loadSingletonExtension(Class<T> type, boolean required) {
        return serviceLocator.loadSingletonImplementor(type, required);
    }

    private void sortExtensions(List<ServiceExtension> extensions, Set<String> loadedExtensions) {
        Map<String, List<ServiceExtension>> mappedExtensions = new HashMap<>();
        extensions.forEach(ext -> ext.provides().forEach(feature -> mappedExtensions.computeIfAbsent(feature, k -> new ArrayList<>()).add(ext)));

        TopologicalSort<ServiceExtension> sort = new TopologicalSort<>();
        extensions.forEach(ext -> ext.requires().forEach(feature -> {
            List<ServiceExtension> dependencies = mappedExtensions.get(feature);
            if (dependencies == null && !loadedExtensions.contains(feature)) {
                throw new DagxException(format("Extension feature required by %s not found: %s", ext.getClass().getName(), feature));
            } else if (dependencies != null) {
                dependencies.forEach(dependency -> sort.addDependency(ext, dependency));
            }
        }));
        sort.sort(extensions);
    }

}
