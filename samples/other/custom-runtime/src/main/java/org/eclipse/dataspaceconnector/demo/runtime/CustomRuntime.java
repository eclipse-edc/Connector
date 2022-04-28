/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.demo.runtime;

import org.eclipse.dataspaceconnector.boot.system.DefaultServiceExtensionContext;
import org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CustomRuntime extends BaseRuntime {

    /**
     * The {@code main} method must be re-implemented, otherwise {@link BaseRuntime#main(String[])} would be called, which would
     * instantiate the {@code BaseRuntime}.
     */
    public static void main(String[] args) {
        new CustomRuntime().boot();
    }

    @Override
    protected String getRuntimeName(ServiceExtensionContext context) {
        return "CUSTOM-RUNTIME";
    }

    @Override
    protected @NotNull ServiceExtensionContext createContext(TypeManager typeManager, Monitor monitor, Telemetry telemetry) {
        //override the default service extension context with a super customized one
        return new SuperCustomExtensionContext(typeManager, monitor, telemetry, loadConfigurationExtensions());
    }

    @Override
    protected void shutdown() {
        super.shutdown();

        //this is the custom part here:
        monitor.info(" CUSTOM RUNTIME SHUTDOWN ! ");
    }

    private static class SuperCustomExtensionContext extends DefaultServiceExtensionContext {
        SuperCustomExtensionContext(TypeManager typeManager, Monitor monitor, Telemetry telemetry, List<ConfigurationExtension> configurationExtensions) {
            super(typeManager, monitor, telemetry, configurationExtensions);
        }
    }
}
