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

package org.eclipse.edc.boot.system.injection.lifecycle;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.InjectionContainer;
import org.eclipse.edc.spi.system.injection.Injector;

/**
 * Represents an abstract phase in an {@link ServiceExtension}'s lifecycle. Is used by the {@link ExtensionLifecycleManager} to ensure the correct
 * sequence of lifecycle events.
 */
abstract class Phase {
    protected final Injector injector;
    protected final InjectionContainer<ServiceExtension> container;
    protected final ServiceExtensionContext context;
    protected final Monitor monitor;

    protected Phase(Injector injector, InjectionContainer<ServiceExtension> container, ServiceExtensionContext context, Monitor monitor) {
        this.injector = injector;
        this.container = container;
        this.context = context;
        this.monitor = monitor;
    }

    protected Phase(Phase other) {
        this(other.injector, other.container, other.context, other.monitor);
    }

    protected ServiceExtension getTarget() {
        return container.getInjectionTarget();
    }
}
