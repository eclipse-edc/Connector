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

package org.eclipse.dataspaceconnector.boot.system.runtime;


import org.eclipse.dataspaceconnector.boot.monitor.MonitorProvider;
import org.eclipse.dataspaceconnector.boot.system.DefaultServiceExtensionContext;
import org.eclipse.dataspaceconnector.boot.system.ExtensionLoader;
import org.eclipse.dataspaceconnector.boot.system.ServiceLocator;
import org.eclipse.dataspaceconnector.boot.system.ServiceLocatorImpl;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionContainer;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.boot.system.ExtensionLoader.loadTelemetry;

/**
 * Base runtime class. During its {@code main()} method it instantiates a new {@code BaseRuntime} object that bootstraps
 * the connector. It goes through the following steps, all of which are overridable:
 * <ul>
 *     <li>{@link BaseRuntime#createTypeManager()}: instantiates a new {@link TypeManager}</li>
 *     <li>{@link BaseRuntime#createMonitor()} : instantiates a new {@link Monitor}</li>
 *     <li>{@link BaseRuntime#createContext(TypeManager, Monitor, Telemetry)}: creates a new {@link DefaultServiceExtensionContext} and invokes its {@link DefaultServiceExtensionContext#initialize()} method</li>
 *     <li>{@link BaseRuntime#createExtensions()}: creates a list of {@code ServiceExtension} objects. By default, these are created through {@link ExtensionLoader#loadServiceExtensions()}</li>
 *     <li>{@link BaseRuntime#bootExtensions(ServiceExtensionContext, List)}: initializes the service extensions by putting them through their lifecycle.
 *     By default this calls {@link ExtensionLoader#bootServiceExtensions(List, ServiceExtensionContext)} </li>
 *     <li>{@link BaseRuntime#onError(Exception)}: receives any Exception that was raised during initialization</li>
 * </ul>
 */
public class BaseRuntime {

    protected final ServiceLocator serviceLocator;
    private final AtomicReference<HealthCheckResult> startupStatus = new AtomicReference<>(HealthCheckResult.failed("Startup not complete"));
    private final ExtensionLoader extensionLoader;
    protected Monitor monitor;
    private final List<ServiceExtension> serviceExtensions = new ArrayList<>();

    public BaseRuntime() {
        this(new ServiceLocatorImpl());
    }

    protected BaseRuntime(ServiceLocator serviceLocator) {
        extensionLoader = new ExtensionLoader(serviceLocator);
        this.serviceLocator = serviceLocator;
    }

    public static void main(String[] args) {
        BaseRuntime runtime = new BaseRuntime();
        runtime.boot();
    }

    protected Monitor getMonitor() {
        return monitor;
    }

    /**
     * Main entry point to runtime initialization. Calls all methods
     * and sets up a context shutdown hook at runtime shutdown.
     */
    protected void boot() {
        boot(true);
    }

    /**
     * Main entry point to runtime initialization. Calls all methods.
     */
    protected void bootWithoutShutdownHook() {
        boot(false);
    }

    @NotNull
    protected ServiceExtensionContext createServiceExtensionContext() {
        var typeManager = createTypeManager();
        monitor = createMonitor();
        MonitorProvider.setInstance(monitor);

        var telemetry = loadTelemetry();

        var context = createContext(typeManager, monitor, telemetry);
        initializeContext(context);
        return context;
    }

    /**
     * Initializes the context. If {@link BaseRuntime#createContext(TypeManager, Monitor, Telemetry)} is overridden and the (custom) context
     * needs to be initialized, this method should be overridden as well.
     *
     * @param context The context.
     */
    protected void initializeContext(ServiceExtensionContext context) {
        context.initialize();
    }

    /**
     * The name of this runtime. This string is solely used for cosmetic/display/logging purposes.
     * By default, {@link ServiceExtensionContext#getConnectorId()} is used.
     */
    protected String getRuntimeName(ServiceExtensionContext context) {
        return context.getConnectorId();
    }

    /**
     * Callback for any error that happened during runtime initialization
     */
    protected void onError(Exception e) {
        monitor.severe("Error booting runtime", e);
        System.exit(-1);  // stop the process
    }

    /**
     * Starts all service extensions by invoking {@link ExtensionLoader#bootServiceExtensions(List, ServiceExtensionContext)}
     *
     * @param context           The {@code ServiceExtensionContext} that is used in this runtime.
     * @param serviceExtensions a list of extensions
     */
    protected void bootExtensions(ServiceExtensionContext context, List<InjectionContainer<ServiceExtension>> serviceExtensions) {
        ExtensionLoader.bootServiceExtensions(serviceExtensions, context);
    }

    /**
     * Create a list of {@link ServiceExtension}s. By default this is done using the ServiceLoader mechanism. Override if
     * e.g. a custom DI mechanism should be used.
     *
     * @return a list of {@code ServiceExtension}s
     */
    protected List<InjectionContainer<ServiceExtension>> createExtensions() {
        return extensionLoader.loadServiceExtensions();
    }

    /**
     * Create a {@link ServiceExtensionContext} that will be used in this runtime. If e.g. a third-party dependency-injection framework were to be used,
     * this would likely need to be overridden.
     *
     * @param typeManager The TypeManager (for JSON de-/serialization)
     * @param monitor     a Monitor
     * @return a {@code ServiceExtensionContext}
     */
    @NotNull
    protected ServiceExtensionContext createContext(TypeManager typeManager, Monitor monitor, Telemetry telemetry) {
        return new DefaultServiceExtensionContext(typeManager, monitor, telemetry, loadConfigurationExtensions());
    }

    protected List<ConfigurationExtension> loadConfigurationExtensions() {
        return extensionLoader.loadExtensions(ConfigurationExtension.class, false);
    }

    /**
     * Hook that is called when a runtime is shutdown (e.g. after a CTRL-C command on a command line). It is highly advisable to
     * forward this signal to all extensions through their {@link ServiceExtension#shutdown()} callback.
     */
    protected void shutdown() {
        var iter = serviceExtensions.listIterator(serviceExtensions.size());
        while (iter.hasPrevious()) {
            var extension = iter.previous();
            extension.shutdown();
            monitor.info("Shutdown " + extension.name());
            iter.remove();
        }
        monitor.info("Shutdown complete");
    }

    /**
     * Hook point to instantiate a {@link Monitor}. By default, the runtime instantiates a {@code Monitor} using the Service Loader mechanism, i.e. by calling the {@link ExtensionLoader#loadMonitor()} method.
     * <p>
     * Please consider using the extension mechanism (i.e. {@link org.eclipse.dataspaceconnector.spi.system.MonitorExtension}) rather than supplying a custom monitor by overriding this method.
     * However, for development/testing scenarios it might be an easy solution to just override this method.
     */
    @NotNull
    protected Monitor createMonitor() {
        return ExtensionLoader.loadMonitor();
    }

    /**
     * Hook point to supply a (custom) TypeManager. By default a new TypeManager is created
     */
    @NotNull
    protected TypeManager createTypeManager() {
        return new TypeManager();
    }

    private void boot(boolean addShutdownHook) {
        ServiceExtensionContext context = createServiceExtensionContext();

        var name = getRuntimeName(context);
        try {
            List<InjectionContainer<ServiceExtension>> newExtensions = createExtensions();
            bootExtensions(context, newExtensions);

            newExtensions.stream().map(InjectionContainer::getInjectionTarget).forEach(serviceExtensions::add);
            if (addShutdownHook) {
                getRuntime().addShutdownHook(new Thread(this::shutdown));
            }

            var healthCheckService = context.getService(HealthCheckService.class);
            healthCheckService.addStartupStatusProvider(this::getStartupStatus);

            startupStatus.set(HealthCheckResult.success());

            healthCheckService.refresh();
        } catch (Exception e) {
            onError(e);
        }

        monitor.info(format("%s ready", name));
    }

    private HealthCheckResult getStartupStatus() {
        return startupStatus.get();
    }

}
