package com.microsoft.dagx.system;

import com.microsoft.dagx.spi.DagxException;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class ServiceLocatorImpl implements ServiceLocator {
    @Override
    public <T> List<T> loadImplementors(Class<T> type, boolean required) {
        List<T> classes = new ArrayList<>();
        ServiceLoader.load(type).iterator().forEachRemaining(classes::add);
        if (classes.isEmpty() && required) {
            throw new DagxException("No classes found of type:  " + type.getName());
        }
        return classes;
    }

    @Override
    public <T> T loadSingletonImplementor(Class<T> type, boolean required) {
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
}
