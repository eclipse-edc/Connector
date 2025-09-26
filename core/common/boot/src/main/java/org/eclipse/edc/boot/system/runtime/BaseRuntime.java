/*
 *  Copyright (c) 2020 - 2024 Microsoft Corporation
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

package org.eclipse.edc.boot.system.runtime;


import org.eclipse.edc.boot.config.ConfigurationLoader;
import org.eclipse.edc.boot.config.EnvironmentVariables;
import org.eclipse.edc.boot.config.SystemProperties;
import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.boot.system.DependencyGraph;
import org.eclipse.edc.boot.system.ExtensionLoader;
import org.eclipse.edc.boot.system.ServiceLocator;
import org.eclipse.edc.boot.system.ServiceLocatorImpl;
import org.eclipse.edc.boot.system.injection.lifecycle.ExtensionLifecycleManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.MonitorExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;

/**
 * Base runtime class. During its {@code main()} method it instantiates a new {@code BaseRuntime} object that bootstraps
 * the connector. It goes through the following steps, all of which are overridable:
 * <ul>
 *     <li>{@link BaseRuntime#createMonitor()} : instantiates a new {@link Monitor}</li>
 *     <li>{@link BaseRuntime#createContext(Monitor, Config)}: creates a new {@link DefaultServiceExtensionContext} and invokes its {@link DefaultServiceExtensionContext#initialize()} method</li>
 *     <li>{@link BaseRuntime#buildDependencyGraph(ServiceExtensionContext)}: creates a list of {@code ServiceExtension} objects. By default, these are created through {@link ExtensionLoader#buildDependencyGraph(ServiceExtensionContext)}</li>
 *     <li>{@link BaseRuntime#bootExtensions(ServiceExtensionContext, DependencyGraph)}: initializes the service extensions by putting them through their lifecycle.
 *     By default, this calls {@link ExtensionLifecycleManager#bootServiceExtensions(List, ServiceExtensionContext)} </li>
 *     <li>{@link BaseRuntime#onError(Exception)}: receives any Exception that was raised during initialization</li>
 * </ul>
 */
public class BaseRuntime {

    private static String[] programArgs = new String[0];
    private final ExtensionLoader extensionLoader;
    private final ConfigurationLoader configurationLoader;
    private List<ServiceExtension> serviceExtensions = new ArrayList<>();
    protected Monitor monitor;
    protected ServiceExtensionContext context;

    public BaseRuntime() {
        this(new ServiceLocatorImpl());
    }

    protected BaseRuntime(ServiceLocator serviceLocator) {
        extensionLoader = new ExtensionLoader(serviceLocator);
        configurationLoader = new ConfigurationLoader(serviceLocator, EnvironmentVariables.ofDefault(), SystemProperties.ofDefault());
    }

    public static void main(String[] args) {
        programArgs = args;
        var runtime = new BaseRuntime();
        runtime.boot(true);
    }

    /**
     * Main entry point to runtime initialization.
     *
     * @param addShutdownHook add a shutdown hook if true, don't otherwise.
     */
    public void boot(boolean addShutdownHook) {
        monitor = createMonitor();
        monitor.info("Booting EDC runtime");

        var config = configurationLoader.loadConfiguration(monitor);
        context = createServiceExtensionContext(config);

        var graph = buildDependencyGraph(context);

        if (!graph.isValid()) {
            graph.getInjectionFailures()
                    .map(failure -> "%s is required by extension %s".formatted(failure.failureDetail(), failure.dependent().name()))
                    .forEach(monitor::severe);
            throw new EdcException("Problems occurred during dependency injection");
        }

        try {
            bootExtensions(context, graph);

            serviceExtensions = graph.getExtensions();

            if (addShutdownHook) {
                getRuntime().addShutdownHook(new Thread(this::shutdown));
            }

            if (context.hasService(HealthCheckService.class)) {
                var startupStatusRef = new AtomicReference<>(HealthCheckResult.Builder.newInstance().component("BaseRuntime").success().build());
                var healthCheckService = context.getService(HealthCheckService.class);
                healthCheckService.addStartupStatusProvider(startupStatusRef::get);
            }

        } catch (Throwable e) {
            onError(e);
        }

        monitor.info(format("Runtime %s ready", context.getRuntimeId()));
    }

    /**
     * Hook that is called when a runtime gets stopped (e.g. after a CTRL-C on a command line). Takes care to signal
     * shutdown to all the extensions in reverse order to the boot.
     */
    public void shutdown() {
        onAllExtensionsInReverseOrder("Shutdown", ServiceExtension::shutdown);
        onAllExtensionsInReverseOrder("Cleanup", ServiceExtension::cleanup);
    }

    protected Monitor getMonitor() {
        return monitor;
    }

    @NotNull
    protected ServiceExtensionContext createServiceExtensionContext(Config config) {
        var context = createContext(monitor, config);
        context.initialize();
        return context;
    }

    /**
     * Callback for any error that happened during runtime initialization
     */
    protected void onError(Throwable e) {
        monitor.severe(String.format("Error booting runtime: %s", e.getMessage()), e);
        throw new EdcException(e);
    }

    /**
     * Starts all service extensions by invoking {@link ExtensionLifecycleManager#bootServiceExtensions(List, ServiceExtensionContext)}
     *
     * @param context The {@code ServiceExtensionContext} that is used in this runtime.
     * @param graph   a list of extensions
     */
    protected void bootExtensions(ServiceExtensionContext context, DependencyGraph graph) {
        ExtensionLifecycleManager.bootServiceExtensions(graph.getInjectionContainers(), context);
    }

    /**
     * Create a list of {@link ServiceExtension}s. By default, this is done using the ServiceLoader mechanism. Override if
     * e.g. a custom DI mechanism should be used.
     *
     * @return a list of {@code ServiceExtension}s
     */
    protected DependencyGraph buildDependencyGraph(ServiceExtensionContext context) {
        return extensionLoader.buildDependencyGraph(context);
    }

    /**
     * Create a {@link ServiceExtensionContext} that will be used in this runtime. If e.g. a third-party dependency-injection framework were to be used,
     * this would likely need to be overridden.
     *
     * @param monitor a Monitor
     * @param config  the configuration
     * @return a {@code ServiceExtensionContext}
     */
    @NotNull
    protected ServiceExtensionContext createContext(Monitor monitor, Config config) {
        return new DefaultServiceExtensionContext(monitor, config);
    }

    /**
     * Hook point to instantiate a {@link Monitor}. By default, the runtime instantiates a {@code Monitor} using the
     * Service Loader mechanism, i.e. by calling the {@link ExtensionLoader#loadMonitor(String...)} method.
     * <p>
     * Please consider using the extension mechanism (i.e. {@link MonitorExtension}) rather than supplying a custom monitor by overriding this method.
     * However, for development/testing scenarios it might be an easy solution to just override this method.
     */
    @NotNull
    protected Monitor createMonitor() {
        return extensionLoader.loadMonitor(programArgs);
    }

    private void onAllExtensionsInReverseOrder(String name, Consumer<ServiceExtension> operation) {
        var iterator = serviceExtensions.listIterator(serviceExtensions.size());
        while (iterator.hasPrevious()) {
            var extension = iterator.previous();
            try {
                monitor.debug("%s %s".formatted(name, extension.name()));
                operation.accept(extension);
            } catch (Throwable e) {
                monitor.severe("Error during extension %s (%s) %s".formatted(extension.name(), extension.getClass().getSimpleName(), name), e);
            }
        }
        monitor.info("%s complete".formatted(name));
    }

}
