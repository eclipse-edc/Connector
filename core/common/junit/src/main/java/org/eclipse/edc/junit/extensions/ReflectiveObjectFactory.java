/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.junit.extensions;

import org.eclipse.edc.boot.system.injection.InjectionContainer;
import org.eclipse.edc.boot.system.injection.InjectionPointScanner;
import org.eclipse.edc.boot.system.injection.Injector;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

/**
 * This is a {@link ObjectFactory} that uses reflection and dependency injection to construct object instances.
 */
public class ReflectiveObjectFactory implements ObjectFactory {
    private final Injector injector;
    private final ServiceExtensionContext context;
    private final InjectionPointScanner injectionPointScanner;

    public ReflectiveObjectFactory(Injector injector, InjectionPointScanner injectionPointScanner, ServiceExtensionContext context) {
        this.injector = injector;
        this.injectionPointScanner = injectionPointScanner;
        this.context = context;
    }


    @Override
    public <T> @NotNull T constructInstance(Class<T> clazz) {
        var instance = getInstance(clazz); // will throw an exception e.g. if no suitable default CTor is found
        var ic = createInjectionContainer(instance);
        injector.inject(ic, context);
        return instance;
    }

    private <T> @NotNull InjectionContainer<T> createInjectionContainer(T instance) {
        return new InjectionContainer<>(instance, injectionPointScanner.getInjectionPoints(instance).collect(toSet()));
    }

    @NotNull
    private <T> T getInstance(Class<T> clazz) {
        try {
            var defaultCtor = getDefaultCtor(clazz);
            defaultCtor.setAccessible(true);
            return defaultCtor.newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new EdcException(e);
        }
    }

    /**
     * attempts to get the parameterless constructor for an object and throws an exception if none is found
     *
     * @param clazz The class of the object
     * @throws NoSuchMethodException if the specified class does not define a default ctor
     */
    @NotNull
    private <T> Constructor<T> getDefaultCtor(Class<T> clazz) throws NoSuchMethodException {
        return Arrays.stream(clazz.getConstructors()).filter(c -> c.getParameterCount() == 0)
                .findFirst()
                .map(c -> (Constructor<T>) c)
                .orElseThrow(() -> new NoSuchMethodException(format("Class %s does not have a parameterless public default constructor!", clazz)));
    }


}
