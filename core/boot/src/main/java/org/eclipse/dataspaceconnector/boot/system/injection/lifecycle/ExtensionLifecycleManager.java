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

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionContainer;
import org.eclipse.dataspaceconnector.spi.system.injection.Injector;

/**
 * {@link ServiceExtension} implementors should not be constructed by just invoking their constructors, instead they need to go through
 * a lifecycle, which is what this class aims at doing. There are three major phases for initialization:
 * <ol>
 *     <li>inject dependencies: all fields annotated with {@link org.eclipse.dataspaceconnector.spi.system.Inject} are set</li>
 *     <li>initialize: invokes the {@link ServiceExtension#initialize(ServiceExtensionContext)} method</li>
 *     <li>provide: invokes all methods annotated with {@link org.eclipse.dataspaceconnector.spi.system.Provider} to register more services into the context</li>
 * </ol>
 * <p>
 * The sequence of these phases is actually important.
 * <p>
 * It is advisable to put all {@link ServiceExtension} instances through their initialization lifecycle <em>before</em> invoking their
 * {@linkplain ServiceExtension#start()} method!
 *
 * @see InitializePhase
 * @see RegistrationPhase
 * @see StartPhase
 */
public class ExtensionLifecycleManager {
    private final InjectionContainer<ServiceExtension> container;
    private final ServiceExtensionContext context;
    private final Injector injector;
    private final Monitor monitor;

    public ExtensionLifecycleManager(InjectionContainer<ServiceExtension> container, ServiceExtensionContext context, Injector injector) {
        monitor = context.getMonitor();
        this.container = container;
        this.context = context;
        this.injector = injector;
    }

    /**
     * Invokes the {@link ServiceExtensionContext#initialize()} method and validates, that every type provided with @Provides
     * is actually provided, logs a warning otherwise
     */
    public static RegistrationPhase initialize(InitializePhase phase) {
        phase.initialize();
        return new RegistrationPhase(phase);
    }

    /**
     * Scans the {@linkplain ServiceExtension} for methods annotated with {@linkplain org.eclipse.dataspaceconnector.spi.system.Provider}
     * with the {@link Provider#isDefault()} flag set to {@code false}, invokes them and registers the bean into the {@link ServiceExtensionContext} if necessary.
     */
    public static StartPhase provide(RegistrationPhase phase) {
        phase.invokeProviderMethods();
        return new StartPhase(phase);
    }

    /**
     * invokes {@link ServiceExtension#start()}.
     */
    public static void start(StartPhase starter) {
        starter.start();
    }

    /**
     * Injects all dependencies into a {@link ServiceExtension}: those dependencies must be class members annotated with @Inject.
     * Kicks off the multi-phase initialization.
     */
    public InitializePhase inject() {
        injector.inject(container, context);
        return new InitializePhase(injector, container, context, monitor);
    }

}
