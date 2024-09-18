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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.boot.system;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.eclipse.edc.boot.monitor.MultiplexingMonitor;
import org.eclipse.edc.boot.system.injection.InjectionContainer;
import org.eclipse.edc.boot.system.injection.InjectorImpl;
import org.eclipse.edc.boot.system.injection.ProviderMethod;
import org.eclipse.edc.boot.system.injection.ProviderMethodScanner;
import org.eclipse.edc.boot.system.injection.lifecycle.ExtensionLifecycleManager;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.MonitorExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtensionLoader {

    private final ServiceLocator serviceLocator;

    public ExtensionLoader(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    /**
     * Convenience method for loading service extensions.
     */
    public static void bootServiceExtensions(List<InjectionContainer<ServiceExtension>> containers, ServiceExtensionContext context) {
        //construct a list of default providers, which are invoked, if a particular service is not present in the context
        var defaultServices = new HashMap<Class<?>, Supplier<Object>>();
        containers.forEach(se -> {
            var pm = new ProviderMethodScanner(se.getInjectionTarget()).defaultProviders();
            pm.forEach(p -> defaultServices.put(p.getReturnType(), getDefaultProviderInvoker(context, se, p)));
        });

        var injector = new InjectorImpl(defaultServices);

        // go through the extension initialization lifecycle
        var lifeCycles = containers.stream()
                .map(c -> new ExtensionLifecycleManager(c, context, injector))
                .map(ExtensionLifecycleManager::inject)
                .map(ExtensionLifecycleManager::initialize)
                .map(ExtensionLifecycleManager::provide)
                .collect(Collectors.toList());

        context.freeze();

        var preparedExtensions = lifeCycles.stream().map(ExtensionLifecycleManager::prepare).collect(Collectors.toList());
        preparedExtensions.forEach(ExtensionLifecycleManager::start);
    }

    @NotNull
    private static Supplier<Object> getDefaultProviderInvoker(ServiceExtensionContext context, InjectionContainer<ServiceExtension> se, ProviderMethod p) {
        return () -> {
            var d = p.invoke(se.getInjectionTarget(), context);
            context.registerService(p.getReturnType(), d);
            return d;
        };
    }

    public static @NotNull Monitor loadMonitor(String... programArgs) {
        var loader = ServiceLoader.load(MonitorExtension.class);
        return loadMonitor(loader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList()), programArgs);
    }

    static @NotNull Monitor loadMonitor(List<MonitorExtension> availableMonitors, String... programArgs) {
        if (availableMonitors.isEmpty()) {
            return new ConsoleMonitor(parseLogLevel(programArgs), !Set.of(programArgs).contains(ConsoleMonitor.COLOR_PROG_ARG));
        }

        if (availableMonitors.size() > 1) {
            return new MultiplexingMonitor(availableMonitors.stream().map(MonitorExtension::getMonitor).collect(Collectors.toList()));
        }

        return availableMonitors.get(0).getMonitor();
    }

    public static @NotNull Telemetry loadTelemetry() {
        var loader = ServiceLoader.load(OpenTelemetry.class);
        var openTelemetries = loader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
        return new Telemetry(selectOpenTelemetryImpl(openTelemetries));
    }

    static @NotNull OpenTelemetry selectOpenTelemetryImpl(List<OpenTelemetry> openTelemetries) {
        if (openTelemetries.size() > 1) {
            throw new IllegalStateException(String.format("Found %s OpenTelemetry implementations. Please provide only one OpenTelemetry service provider.", openTelemetries.size()));
        }
        return openTelemetries.isEmpty() ? GlobalOpenTelemetry.get() : openTelemetries.get(0);
    }

    /**
     * Loads and orders the service extensions.
     */
    public List<InjectionContainer<ServiceExtension>> loadServiceExtensions(ServiceExtensionContext context) {
        List<ServiceExtension> serviceExtensions = loadExtensions(ServiceExtension.class, true);
        return new DependencyGraph(context).of(serviceExtensions);
    }

    /**
     * Loads multiple extensions, raising an exception if at least one is not found.
     */
    public <T> List<T> loadExtensions(Class<T> type, boolean required) {
        return serviceLocator.loadImplementors(type, required);
    }

    /**
     * Parses the ConsoleMonitor log level from the program args. If no log level is provided, defaults to Level default.
     */
    private static ConsoleMonitor.Level parseLogLevel(String[] programArgs) {
        return Stream.of(programArgs)
                .filter(arg -> arg.startsWith(ConsoleMonitor.LEVEL_PROG_ARG + "="))
                .map(arg -> arg.split("=").length == 2 ? arg.split("=")[1] : "null" )
                .map(lvl -> {
                    try {
                        return ConsoleMonitor.Level.valueOf(lvl);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(String.format("Illegal Console Monitor log level value: %s. Possible values are %s", lvl, Stream.of(ConsoleMonitor.Level.values()).toList()));
                    }
                })
                .findFirst()
                .orElse(ConsoleMonitor.Level.getDefaultLevel());
    }

}
