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

package org.eclipse.dataspaceconnector.boot.system.injection.lifecycle;

import org.eclipse.dataspaceconnector.boot.system.ExtensionLoader;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionContainer;
import org.eclipse.dataspaceconnector.spi.system.injection.Injector;
import org.eclipse.dataspaceconnector.spi.system.injection.ProviderMethodScanner;

import static java.lang.String.format;

/**
 * {@link ServiceExtension} implementors should not be constructed by just invoking their constructors, instead they need to go through
 * a lifecycle, which is what this class aims at doing. There are three major phases for initialization:
 * <ol>
 *     <li>inject dependencies: all fields annotated with {@link org.eclipse.dataspaceconnector.spi.system.Inject} are set</li>
 *     <li>provide: invokes all methods annotated with {@link org.eclipse.dataspaceconnector.spi.system.Provider} to register more services into the context</li>
 *     <li>initialize: invokes the {@link ServiceExtension#initialize(ServiceExtensionContext)} method</li>
 * </ol>
 * <p>
 * The sequence of these phases is actually important.
 * <p>
 * It is advisable to put all {@link ServiceExtension} instances through their initialization lifecycle <em>before</em> invoking their
 * {@linkplain ServiceExtension#start()} method!
 *
 */
public class ExtensionLifecycleManager {
    private final InjectionContainer<ServiceExtension> container;
    private final ServiceExtensionContext context;
    private final Injector injector;
    private final ProviderMethodScanner providerMethodScanner;
    private final Monitor monitor;

    public ExtensionLifecycleManager(InjectionContainer<ServiceExtension> container, ServiceExtensionContext context, Injector injector, ProviderMethodScanner providerMethodScanner) {
        monitor = context.getMonitor();
        this.container = container;
        this.context = context;
        this.injector = injector;
        this.providerMethodScanner = providerMethodScanner;
    }

    /**
     * Injects all dependencies into a {@link ServiceExtension}: those dependencies must be class members annotated with @Inject.
     * Kicks off the multi-phase initialization.
     */
    public ExtensionLifecycleManager inject() {
        injector.inject(container, context);
        return this;
    }

    /**
     * Invokes the {@link ServiceExtensionContext#initialize()} method and validates, that every type provided with @Provides
     * is actually provided, logs a warning otherwise
     */
    public ExtensionLifecycleManager initialize() {
        // call initialize
        var target = container.getInjectionTarget();
        target.initialize(context);
        var result = container.validate(context);

        // wrap failure message in a more descriptive string
        if (result.failed()) {
            monitor.warning(String.join(", ", format("There were missing service registrations in extension %s: %s", target.getClass(), String.join(", ", result.getFailureMessages()))));
        }
        monitor.info("Initialized " + container.getInjectionTarget().name());
        return this;
    }

    /**
     * Scans the {@linkplain ServiceExtension} for methods annotated with {@linkplain org.eclipse.dataspaceconnector.spi.system.Provider}
     * with the {@link Provider#isDefault()} flag set to {@code false}, invokes them and registers the bean into the {@link ServiceExtensionContext} if necessary.
     */
    public ExtensionLifecycleManager provide() {
        var target = container.getInjectionTarget();
        // invoke provider methods, register the service they return
        providerMethodScanner.nonDefaultProviders().forEach(pm -> ExtensionLoader.getProvidedAndRegister(context, pm, target));
        return this;
    }

    /**
     * invokes {@link ServiceExtension#start()}.
     */
    public ExtensionLifecycleManager start() {
        var target = container.getInjectionTarget();
        target.start();
        monitor.info("Started " + target.name());
        return this;
    }

}
