package com.microsoft.dagx.system;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ConfigurationExtension;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.util.TopologicalSort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static com.microsoft.dagx.spi.system.ServiceExtension.LoadPhase.DEFAULT;
import static com.microsoft.dagx.spi.system.ServiceExtension.LoadPhase.PRIMORDIAL;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

/**
 * Base service extension context.
 *
 * Prior to using, {@link #initialize()} must be called.
 */
public class DefaultServiceExtensionContext implements ServiceExtensionContext {
    private Monitor monitor;
    private TypeManager typeManager;

    private Map<Class<?>, Object> services = new HashMap<>();
    private List<ConfigurationExtension> configurationExtensions;

    public DefaultServiceExtensionContext(TypeManager typeManager, Monitor monitor) {
        this.typeManager = typeManager;
        this.monitor = monitor;
    }

    public void initialize() {
        configurationExtensions = loadExtensions(ConfigurationExtension.class, false);
        configurationExtensions.forEach(ext-> ext.initialize(monitor));
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
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> type) {
        T service = (T) services.get(type);
        if (service == null) {
            throw new DagxException("Service not found: " + type.getName());
        }
        return service;
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

        sortExtensions(primordialExtensions);
        sortExtensions(defaultExtensions);

        List<ServiceExtension> totalOrdered = new ArrayList<>(primordialExtensions);
        totalOrdered.addAll(defaultExtensions);
        return totalOrdered;
    }

    @Override
    public <T> List<T> loadExtensions(Class<T> type, boolean required) {
        List<T> extensions = new ArrayList<>();
        ServiceLoader.load(type).iterator().forEachRemaining(extensions::add);
        if (extensions.isEmpty() && required) {
            throw new DagxException("No extensions found of type:  " + type.getName());
        }
        return extensions;
    }

    @Override
    public <T> T loadSingletonExtension(Class<T> type, boolean required) {
        List<T> extensions = new ArrayList<>();
        ServiceLoader.load(type).iterator().forEachRemaining(extensions::add);
        if (extensions.isEmpty() && required) {
            throw new DagxException("No extensions found of type:  " + type.getName());
        } else if (extensions.size() > 1) {
            String types = extensions.stream().map(e -> e.getClass().getName()).collect(joining(","));
            throw new DagxException(format("Multiple extensions found of type: %s [%s]", type.getName(), types));
        }
        return !extensions.isEmpty() ? extensions.get(0) : null;
    }

    private void sortExtensions(List<ServiceExtension> extensions) {
        Map<String, List<ServiceExtension>> mappedExtensions = new HashMap<>();
        extensions.forEach(ext -> ext.provides().forEach(feature -> mappedExtensions.computeIfAbsent(feature, k -> new ArrayList<>()).add(ext)));

        TopologicalSort<ServiceExtension> sort = new TopologicalSort<>();
        extensions.forEach(ext -> ext.requires().forEach(feature -> {
            List<ServiceExtension> dependencies = mappedExtensions.get(feature);
            if (dependencies == null) {
                throw new DagxException(format("Extension feature required by %s not found: %s", ext.getClass().getName(), feature));
            }
            dependencies.forEach(dependency -> sort.addDependency(ext, dependency));
        }));
        sort.sort(extensions);
    }

}
