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
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


/**
 * Converts an unsorted list of {@link ServiceExtension} instances into a directed graph based on dependency direction, i.e.
 * which extension depends on which other extension.
 */
public class DependencyGraph {

    private final List<InjectionContainer<ServiceExtension>> injectionContainers;
    /**
     * contains all missing dependencies that were expressed as injection points
     */
    private final HashMap<Class<? extends ServiceExtension>, List<InjectionFailure>> unsatisfiedInjectionPoints;
    /**
     * contains all missing dependencies that were expressed as @Require(...) annotations on the extension class
     */
    private final ArrayList<Class<?>> unsatisfiedRequirements;

    private DependencyGraph(List<InjectionContainer<ServiceExtension>> injectionContainers, HashMap<Class<? extends ServiceExtension>, List<InjectionFailure>> unsatisfiedInjectionPoints, ArrayList<Class<?>> unsatisfiedRequirements) {

        this.injectionContainers = injectionContainers;
        this.unsatisfiedInjectionPoints = unsatisfiedInjectionPoints;
        this.unsatisfiedRequirements = unsatisfiedRequirements;
    }

    /**
     * Builds the DependencyGraph by evaluating all {@link ServiceExtension} implementors, that were found on the classpath,
     * and sorting them topologically according to their dependencies.
     * <em>Dependent</em> extensions (i.e. those who <em>express</em> a dependency) are sorted first, providing extensions (i.e. those
     * who <em>provide</em> a dependency) are sorted last.
     * <p>
     * This factory method does not throw any exception except a {@link CyclicDependencyException}, please check {@link DependencyGraph#isValid()} if the graph is valid.
     *
     * @param context    An instance of the (fully-initialized) {@link ServiceExtensionContext} which is used to resolve services and configuration.
     * @param extensions A list of {@link ServiceExtension} instances that were picked up by the {@link ServiceLocator}
     * @return A list of {@link InjectionContainer}s that are sorted topologically according to their dependencies.
     * @throws CyclicDependencyException when there is a dependency cycle
     * @see TopologicalSort
     * @see InjectionContainer
     */
    public static DependencyGraph of(ServiceExtensionContext context, List<ServiceExtension> extensions) {
        var injectionPointScanner = new InjectionPointScanner();

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
        var unsatisfiedRequirements = new ArrayList<Class<?>>();

        injectionContainers.forEach(container -> {
            //check that all the @Required features are there
            getRequiredFeatures(container.getInjectionTarget().getClass()).forEach(serviceClass -> {
                var dependencies = dependencyMap.get(serviceClass);
                if (dependencies == null) {
                    unsatisfiedRequirements.add(serviceClass);
                } else {
                    dependencies.forEach(dependency -> sort.addDependency(container, dependency));
                }
            });

            injectionPointScanner.getInjectionPoints(container.getInjectionTarget())
                    .peek(injectionPoint -> {
                        injectionPoint.getProviders(dependencyMap, context)
                                .onSuccess(providers -> providers.stream()
                                        .filter(d -> !Objects.equals(d, container))
                                        .forEach(provider -> sort.addDependency(container, provider)))
                                .onFailure(f -> {
                                    if (injectionPoint.isRequired()) {
                                        unsatisfiedInjectionPoints.computeIfAbsent(injectionPoint.getTargetInstance().getClass(), s -> new ArrayList<>())
                                                .add(new InjectionFailure(injectionPoint.getTargetInstance(), injectionPoint, f.getFailureDetail()));
                                    }
                                });

                        var defaultServiceProvider = defaultServiceProviders.get(injectionPoint.getType());
                        if (defaultServiceProvider != null) {
                            injectionPoint.setDefaultValueProvider(defaultServiceProvider);
                        }
                    })
                    .forEach(injectionPoint -> container.getInjectionPoints().add(injectionPoint));
        });

        sort.sort(injectionContainers);

        return new DependencyGraph(injectionContainers, unsatisfiedInjectionPoints, unsatisfiedRequirements);
    }

    /**
     * Returns all {@link InjectionPoint}s for a particular extension class. These include all types of injection points.
     *
     * @param serviceClass The extension class
     * @return A (potentially empty) list of injection points.
     */
    public List<InjectionPoint<ServiceExtension>> getDependenciesOf(Class<? extends ServiceExtension> serviceClass) {
        return injectionContainers.stream().filter(ic -> ic.getInjectionTarget().getClass().equals(serviceClass))
                .flatMap(ic -> ic.getInjectionPoints().stream())
                .toList();
    }

    /**
     * Obtains all extension classes that declare a dependency on an object of the given point. For example, declaring a
     * field {@code @Inject FooService fooService} in an extension would constitute such a dependency, and in that case
     * {@code FooService.class} has to be passed into this method.
     * <p>
     * This can also be invoked if the dependency graph is invalid, i.e. if dependency injection is not possible.
     *
     * @param dependencyType The type of the injection point, for example the type of service that is injected.
     * @return a list of extension classes that declare a dependency onto the given injection point
     */
    public List<Class<? extends ServiceExtension>> getDependentExtensions(Class<?> dependencyType) {

        return injectionContainers.stream()
                .filter(ic -> ic.getInjectionPoints().stream().anyMatch(ip -> ip.getType().equals(dependencyType)))
                .map(InjectionContainer::getInjectionTarget)
                .map((Function<ServiceExtension, Class<? extends ServiceExtension>>) ServiceExtension::getClass) // yes, it's nasty, but keeps the compiler happy
                .toList();
    }

    /**
     * Obtains a list of injection points, where the injection target is of the given type. For example, passing in {@code FooService.class}
     * would return a list that contains injection points across all {@link ServiceExtension}s that declare a {@code @Inject FooService fooService} field.
     * This is similar to {@link DependencyGraph#getDependenciesOf(Class)}, but the result values are {@link InjectionPoint}s rather than extension classes.
     *
     * @param dependencyType The type of the injection point, for example the type of service that is injected.
     * @return a list of {@link InjectionPoint} instances that represent the concrete dependency
     */
    public List<InjectionPoint<ServiceExtension>> getDependenciesFor(Class<?> dependencyType) {
        return injectionContainers.stream()
                .flatMap(ic -> ic.getInjectionPoints().stream().filter(ip -> ip.getType().equals(dependencyType)))
                .toList();
    }

    public List<InjectionContainer<ServiceExtension>> getInjectionContainers() {
        return injectionContainers;
    }

    /**
     * Returns a list of extension instances that were found on the classpath
     */
    public List<ServiceExtension> getExtensions() {
        return injectionContainers.stream().map(InjectionContainer::getInjectionTarget).toList();
    }

    /**
     * Checks if the current dependency graph is valid, i.e. there are no cycles in it and all required injection
     * dependencies are satisfied.
     *
     * @return true if the dependency graph is valid, and the DI container can be built, false otherwise.
     */
    public boolean isValid() {
        return unsatisfiedInjectionPoints.isEmpty() && unsatisfiedRequirements.isEmpty();
    }

    /**
     * Returns a list of strings, each containing information about a missing dependency
     *
     * @return A list of errors describing one missing dependency each
     */
    public List<String> getProblems() {
        var messages = unsatisfiedInjectionPoints.entrySet().stream()
                .map(entry -> {
                    var dependent = entry.getKey();
                    var dependencies = entry.getValue();
                    var missingDependencyList = dependencies.stream().map(injectionFailure -> "  --> " + injectionFailure.failureDetail()).toList();
                    return "## %s is missing\n%s".formatted(dependent, String.join("\n", missingDependencyList));
                });

        return messages.toList();
    }

    private static Stream<Class<?>> getRequiredFeatures(Class<?> clazz) {
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
    private static Set<Class<?>> getProvidedFeatures(ServiceExtension ext) {
        var allProvides = new HashSet<Class<?>>();

        // check all @Provides
        var providesAnnotation = ext.getClass().getAnnotation(Provides.class);
        if (providesAnnotation != null) {
            allProvides.addAll(Arrays.asList(providesAnnotation.value()));
        }

        return allProvides;
    }

    private record InjectionFailure(ServiceExtension dependent, InjectionPoint<ServiceExtension> dependency,
                                    @Nullable String failureDetail) {
        @Override
        public String toString() {
            return "%s %s".formatted(dependency.getTypeString(), failureDetail);
        }
    }
}
