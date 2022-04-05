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
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionContainer;
import org.eclipse.dataspaceconnector.spi.system.injection.Injector;

import static java.lang.String.format;

/**
 * Represents an {@link ServiceExtension}'s lifecycle phase where it's {@linkplain ServiceExtension#initialize(ServiceExtensionContext)} method is invoked by the
 * {@link ExtensionLifecycleManager}.
 */
public class InitializePhase extends Phase {
    protected InitializePhase(Injector injector, InjectionContainer<ServiceExtension> container, ServiceExtensionContext context, Monitor monitor) {
        super(injector, container, context, monitor);
    }

    protected void initialize() {
        // call initialize
        var target = getTarget();
        target.initialize(context);
        var result = container.validate(context);

        // wrap failure message in a more descriptive string
        if (result.failed()) {
            monitor.warning(String.join(", ", format("There were missing service registrations in extension %s: %s", target.getClass(), String.join(", ", result.getFailureMessages()))));
        }
        monitor.info("Initialized " + container.getInjectionTarget().name());
    }
}
