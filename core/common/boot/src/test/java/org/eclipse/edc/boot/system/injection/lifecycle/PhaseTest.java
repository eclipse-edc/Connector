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

import org.eclipse.edc.boot.system.injection.InjectionContainer;
import org.eclipse.edc.boot.system.injection.Injector;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.mock;

class PhaseTest {
    protected Injector injector;
    protected InjectionContainer<ServiceExtension> container;
    protected ServiceExtensionContext context;
    protected Monitor monitor;

    @BeforeEach
    void setup() {
        injector = mock(Injector.class);
        container = mock(InjectionContainer.class);
        context = mock(ServiceExtensionContext.class);
        monitor = mock(Monitor.class);
    }
}
