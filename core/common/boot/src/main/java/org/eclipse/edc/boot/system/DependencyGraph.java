/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.boot.system;

import org.eclipse.edc.boot.system.injection.EdcInjectionException;
import org.eclipse.edc.boot.system.injection.InjectionContainer;
import org.eclipse.edc.boot.system.injection.InjectionPoint;
import org.eclipse.edc.boot.system.injection.InjectionPointScanner;
import org.eclipse.edc.boot.system.injection.ProviderMethod;
import org.eclipse.edc.boot.system.injection.ProviderMethodScanner;
import org.eclipse.edc.boot.system.injection.lifecycle.ServiceProvider;
import org.eclipse.edc.boot.util.CyclicDependencyException;
import org.eclipse.edc.boot.util.TopologicalSort;
import org.eclipse.edc.runtime.metamodel.annotation.BaseExtension;
import org.eclipse.edc.runtime.metamodel.annotation.CoreExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Requires;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;


/**
 * Converts an unsorted list of {@link ServiceExtension} instances into a directed graph based on dependency direction, i.e.
 * which extension depends on which other extension.
 */
public class DependencyGraph {
    private final InjectionPointScanner injectionPointScanner = new InjectionPointScanner();
    private final ServiceExtensionContext context;

    public DependencyGraph(ServiceExtensionContext context) {
        this.context = context;
    }

    /**
     * Sorts all {@link ServiceExtension} implementors, that were found on the classpath, according to their dependencies.
     * Depending Extensions (i.e. those who <em>express</em> a dependency) are sorted first, providing extensions (i.e. those
     * who provide a dependency) are sorted last.
     *
     * @param loadedExtensions A list of {@link ServiceExtension} instances that were picked up by the {@link ServiceLocator}
     * @return A list of {@link InjectionContainer}s that are sorted topologically according to their dependencies.
     * @throws CyclicDependencyException when there is a dependency cycle
     * @see TopologicalSort
     * @see InjectionContainer
     */
    public List<InjectionContainer<ServiceExtension>> of(List<ServiceExtension> loadedExtensions) {
        var extensions = sortByType(loadedExtensions);
        Map<Class<?>, ServiceProvider> defaultServiceProviders = new HashMap<>();
        Map<ServiceExtension, List<ServiceProvider>> serviceProviders = new HashMap<>();
        Map<Class<?>, List<ServiceExtension>> dependencyMap = new HashMap<>();
        extensions.forEach(extension -> {
            getProvidedFeatures(extension).forEach(feature -> dependencyMap.computeIfAbsent(feature, k -> new ArrayList<>()).add(extension));
            // check all @Provider methods
            new ProviderMethodScanner(extension).allProviders()
                    .peek(providerMethod -> {
                        var serviceProvider = new ServiceProvider(providerMethod, extension);
                        if (providerMethod.isDefault()) {
                            defaultServiceProviders.put(providerMethod.getReturnType(), serviceProvider);
                        } else {
                            serviceProviders.computeIfAbsent(extension, k -> new ArrayList<>()).add(serviceProvider);
                        }
                    })
                    .map(ProviderMethod::getReturnType)
                    .forEach(feature -> dependencyMap.computeIfAbsent(feature, k -> new ArrayList<>()).add(extension));
        });

        var sort = new TopologicalSort<ServiceExtension>();

        // check if all injected fields are satisfied, collect missing ones and throw exception otherwise
        var unsatisfiedInjectionPoints = new ArrayList<InjectionPoint<ServiceExtension>>();
        var unsatisfiedRequirements = new ArrayList<String>();

        var injectionPoints = extensions.stream()
                .collect(toMap(identity(), ext -> {

                    //check that all the @Required features are there
                    getRequiredFeatures(ext.getClass()).forEach(serviceClass -> {
                        var dependencies = dependencyMap.get(serviceClass);
                        if (dependencies == null) {
                            unsatisfiedRequirements.add(serviceClass.getName());
                        } else {
                            dependencies.forEach(dependency -> sort.addDependency(ext, dependency));
                        }
                    });

                    return injectionPointScanner.getInjectionPoints(ext)
                            .peek(injectionPoint -> {
                                if (!canResolve(dependencyMap, injectionPoint.getType())) {
                                    if (injectionPoint.isRequired()) {
                                        unsatisfiedInjectionPoints.add(injectionPoint);
                                    }
                                } else {
                                    // get() would return null, if the feature is already in the context's service list
                                    ofNullable(dependencyMap.get(injectionPoint.getType()))
                                            .ifPresent(l -> l.stream()
                                                    .filter(d -> !Objects.equals(d, ext)) // remove dependencies onto oneself
                                                    .forEach(provider -> sort.addDependency(ext, provider)));
                                }

                                var defaultServiceProvider = defaultServiceProviders.get(injectionPoint.getType());
                                if (defaultServiceProvider != null) {
                                    injectionPoint.setDefaultServiceProvider(defaultServiceProvider);
                                }
                            })
                            .collect(toSet());
                }));

        if (!unsatisfiedInjectionPoints.isEmpty()) {
            var string = "The following injected fields were not provided:\n";
            string += unsatisfiedInjectionPoints.stream().map(InjectionPoint::toString).collect(Collectors.joining("\n"));
            throw new EdcInjectionException(string);
        }

        if (!unsatisfiedRequirements.isEmpty()) {
            var string = String.format("The following @Require'd features were not provided: [%s]", String.join(", ", unsatisfiedRequirements));
            throw new EdcException(string);
        }

        sort.sort(extensions);

        // convert the sorted list of extensions into an equally sorted list of InjectionContainers
        return extensions.stream()
                .map(key -> new InjectionContainer<>(key, injectionPoints.get(key), serviceProviders.get(key)))
                .toList();
    }

    private boolean canResolve(Map<Class<?>, List<ServiceExtension>> dependencyMap, Class<?> serviceClass) {
        var providers = dependencyMap.get(serviceClass);
        if (providers != null) {
            return true;
        } else {
            // attempt to interpret the feature name as class name, instantiate it and see if the context has that service
            return context.hasService(serviceClass);
        }
    }

    private Stream<Class<?>> getRequiredFeatures(Class<?> clazz) {
        var requiresAnnotation = clazz.getAnnotation(Requires.class);
        if (requiresAnnotation != null) {
            var features = requiresAnnotation.value();
            return Stream.of(features);
        }
        return Stream.empty();
    }

    /**
     * Obtains all features a specific extension requires as strings
     */
    private Set<Class<?>> getProvidedFeatures(ServiceExtension ext) {
        var allProvides = new HashSet<Class<?>>();

        // check all @Provides
        var providesAnnotation = ext.getClass().getAnnotation(Provides.class);
        if (providesAnnotation != null) {
            allProvides.addAll(Arrays.asList(providesAnnotation.value()));
        }

        return allProvides;
    }

    /**
     * Handles core-, transfer- and contract-extensions and inserts them at the beginning of the list so that
     * explicit @Requires annotations are not necessary
     */
    private List<ServiceExtension> sortByType(List<ServiceExtension> loadedExtensions) {
        return loadedExtensions.stream().sorted(new SortByType()).collect(toList());
    }

    private static class SortByType implements Comparator<ServiceExtension> {
        @Override
        public int compare(ServiceExtension o1, ServiceExtension o2) {
            return orderFor(o1.getClass()).compareTo(orderFor(o2.getClass()));
        }

        private Integer orderFor(Class<? extends ServiceExtension> class1) {
            return class1.getAnnotation(BaseExtension.class) != null
                    ? 0 : class1.getAnnotation(CoreExtension.class) != null
                    ? 1 : 2;
        }
    }
}
