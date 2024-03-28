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

package org.eclipse.edc.boot.system.testextensions;

import org.eclipse.edc.boot.system.ServiceLocator;
import org.eclipse.edc.boot.system.runtime.BaseRuntime;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

public class BaseRuntimeFixture extends BaseRuntime {

    private final Monitor monitor;

    public BaseRuntimeFixture(Monitor monitor, ServiceLocator serviceLocator) {
        super(serviceLocator);
        this.monitor = monitor;
    }

    public void start() {
        bootWithoutShutdownHook();
    }

    public void stop() {
        super.shutdown();
    }

    @Override
    protected @NotNull Monitor createMonitor() {
        return monitor;
    }
}
