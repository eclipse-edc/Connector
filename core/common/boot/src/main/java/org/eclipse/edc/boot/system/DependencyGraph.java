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
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Requires;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


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
     * @param extensions A list of {@link ServiceExtension} instances that were picked up by the {@link ServiceLocator}
     * @return A list of {@link InjectionContainer}s that are sorted topologically according to their dependencies.
     * @throws CyclicDependencyException when there is a dependency cycle
     * @see TopologicalSort
     * @see InjectionContainer
     */
    public List<InjectionContainer<ServiceExtension>> of(List<ServiceExtension> extensions) {
        Map<Class<?>, ServiceProvider> defaultServiceProviders = new HashMap<>();
        Map<Class<?>, List<InjectionContainer<ServiceExtension>>> dependencyMap = new HashMap<>();
        var injectionContainers = extensions.stream()
                .map(it -> new InjectionContainer<>(it, new HashSet<>(), new ArrayList<>()))
                .peek(injectionContainer -> {
                    getProvidedFeatures(injectionContainer.getInjectionTarget())
                            .forEach(feature -> dependencyMap.computeIfAbsent(feature, k -> new ArrayList<>()).add(injectionContainer));

                    // check all @Provider methods
                    new ProviderMethodScanner(injectionContainer.getInjectionTarget()).allProviders()
                            .peek(providerMethod -> {
                                var serviceProvider = new ServiceProvider(providerMethod, injectionContainer.getInjectionTarget());
                                if (providerMethod.isDefault()) {
                                    defaultServiceProviders.put(providerMethod.getReturnType(), serviceProvider);
                                } else {
                                    injectionContainer.getServiceProviders().add(serviceProvider);
                                }
                            })
                            .map(ProviderMethod::getReturnType)
                            .forEach(feature -> dependencyMap.computeIfAbsent(feature, k -> new ArrayList<>()).add(injectionContainer));
                })
                .collect(toList());

        var sort = new TopologicalSort<InjectionContainer<ServiceExtension>>();

        // check if all injected fields are satisfied, collect missing ones and throw exception otherwise
        var unsatisfiedInjectionPoints = new HashMap<Class<? extends ServiceExtension>, List<InjectionFailure>>();
        var unsatisfiedRequirements = new ArrayList<String>();

        injectionContainers.forEach(container -> {
            //check that all the @Required features are there
            getRequiredFeatures(container.getInjectionTarget().getClass()).forEach(serviceClass -> {
                var dependencies = dependencyMap.get(serviceClass);
                if (dependencies == null) {
                    unsatisfiedRequirements.add(serviceClass.getName());
                } else {
                    dependencies.forEach(dependency -> sort.addDependency(container, dependency));
                }
            });

            injectionPointScanner.getInjectionPoints(container.getInjectionTarget())
                    .peek(injectionPoint -> {
                        var providersResult = injectionPoint.getProviders(dependencyMap, context);
                        if (providersResult.succeeded()) {
                            List<InjectionContainer<ServiceExtension>> providers = providersResult.getContent();
                            providers.stream().filter(d -> !Objects.equals(d, container)).forEach(provider -> sort.addDependency(container, provider));
                        } else {
                            if (injectionPoint.isRequired()) {
                                unsatisfiedInjectionPoints.computeIfAbsent(injectionPoint.getTargetInstance().getClass(), s -> new ArrayList<>()).add(new InjectionFailure(injectionPoint, providersResult.getFailureDetail()));
                            }
                        }

                        var defaultServiceProvider = defaultServiceProviders.get(injectionPoint.getType());
                        if (defaultServiceProvider != null) {
                            injectionPoint.setDefaultServiceProvider(defaultServiceProvider);
                        }
                    })
                    .forEach(injectionPoint -> container.getInjectionPoints().add(injectionPoint));
        });

        if (!unsatisfiedInjectionPoints.isEmpty()) {
            var message = "The following injected fields or values were not provided or could not be resolved:\n";
            message += unsatisfiedInjectionPoints.entrySet().stream()
                    .map(entry -> String.format("%s is missing \n  --> %s", entry.getKey(), String.join("\n  --> ", entry.getValue().stream().map(Object::toString).toList()))).collect(Collectors.joining("\n"));
            throw new EdcInjectionException(message);
        }

        if (!unsatisfiedRequirements.isEmpty()) {
            var message = String.format("The following @Require'd features were not provided: [%s]", String.join(", ", unsatisfiedRequirements));
            throw new EdcException(message);
        }

        sort.sort(injectionContainers);

        return injectionContainers;
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

    private record InjectionFailure(InjectionPoint<ServiceExtension> injectionPoint, String failureDetail) {
        @Override
        public String toString() {
            return "%s %s".formatted(injectionPoint.getTypeString(), failureDetail);
        }
    }
}
