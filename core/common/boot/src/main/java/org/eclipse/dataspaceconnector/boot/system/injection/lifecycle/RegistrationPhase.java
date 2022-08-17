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

import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.ProviderMethod;
import org.eclipse.dataspaceconnector.spi.system.injection.ProviderMethodScanner;

/**
 * Represents an {@link ServiceExtension}'s lifecycle phase where all it's provider methods are invoked by the {@link ExtensionLifecycleManager}, and the provided objects are registered into the context.
 *
 * @see ProviderMethodScanner
 */
public class RegistrationPhase extends Phase {

    private final ProviderMethodScanner methodScanner;

    protected RegistrationPhase(Phase other) {
        this(other, new ProviderMethodScanner(other.getTarget()));
    }

    // mainly used for testing
    protected RegistrationPhase(Phase other, ProviderMethodScanner scanner) {
        super(other);
        methodScanner = scanner;
    }

    protected void invokeProviderMethods() {
        var target = getTarget();
        // invoke provider methods, register the service they return
        methodScanner.nonDefaultProviders()
                .forEach(pm -> invokeAndRegister(pm, target, context));
    }

    private void invokeAndRegister(ProviderMethod m, ServiceExtension target, ServiceExtensionContext context) {
        var type = m.getReturnType();

        var res = m.invoke(target, context);
        context.registerService(type, res);
    }
}
