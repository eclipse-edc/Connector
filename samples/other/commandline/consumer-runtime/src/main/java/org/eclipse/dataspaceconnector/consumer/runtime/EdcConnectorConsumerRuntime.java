/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.consumer.runtime;

import org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

public class EdcConnectorConsumerRuntime extends BaseRuntime {

    private ServiceExtensionContext context;

    public void start() {
        boot();
    }

    @Override
    protected @NotNull ServiceExtensionContext createContext(TypeManager typeManager, Monitor monitor, Telemetry telemetry) {
        this.context = super.createContext(typeManager, monitor, telemetry);
        return context;
    }

    public TypeManager getTypeManager() {
        return context.getTypeManager();
    }

    public Monitor getMonitor() {
        return context.getMonitor();
    }

    public <T> T getService(Class<T> type) {
        return context.getService(type);
    }

    public static class Builder {
        private final EdcConnectorConsumerRuntime runtime;

        private Builder() {
            runtime = new EdcConnectorConsumerRuntime();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public EdcConnectorConsumerRuntime build() {
            return runtime;
        }
    }
}