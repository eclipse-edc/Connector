/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.junit.extensions;

import org.eclipse.edc.boot.system.ServiceLocator;
import org.eclipse.edc.boot.system.ServiceLocatorImpl;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.SystemExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.eclipse.edc.util.types.Cast.cast;

/**
 * A service locator that allows additional extensions to be manually loaded by a test fixture. This locator return
 * the union of registered extensions and extensions loaded by the delegate.
 */
class MultiSourceServiceLocator implements ServiceLocator {
    private final ServiceLocator delegate = new ServiceLocatorImpl();
    private final LinkedHashMap<Class<? extends SystemExtension>, List<SystemExtension>> systemExtensions;

    MultiSourceServiceLocator() {
        systemExtensions = new LinkedHashMap<>();
    }

    @Override
    public <T> List<T> loadImplementors(Class<T> type, boolean required) {
        List<T> extensions = cast(systemExtensions.getOrDefault(type, new ArrayList<>()));
        extensions.addAll(delegate.loadImplementors(type, required));
        return extensions;
    }

    /**
     * This implementation will override singleton implementions found by the delegate.
     */
    @Override
    public <T> T loadSingletonImplementor(Class<T> type, boolean required) {
        var extensions = systemExtensions.get(type);
        if (extensions == null || extensions.isEmpty()) {
            return delegate.loadSingletonImplementor(type, required);
        } else if (extensions.size() > 1) {
            throw new EdcException("Multiple extensions were registered for type: " + type.getName());
        }
        return type.cast(extensions.get(0));
    }

    public <T extends SystemExtension> void registerSystemExtension(Class<T> type, SystemExtension extension) {
        systemExtensions.computeIfAbsent(type, k -> new ArrayList<>()).add(extension);
    }

    public void clearSystemExtensions() {
        systemExtensions.clear();
    }
}
