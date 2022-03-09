/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.configuration;

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;

import static java.lang.String.format;

@Provides(IdsApiConfiguration.class)
public class IdsApiConfigurationExtension implements ServiceExtension {
    
    @EdcSetting
    private static final String IDS_API_CONFIG = "web.http.ids";
    private static final String IDS_API_CONTEXT_ALIAS = "ids";
    
    private static final String DEFAULT_API_CONTEXT_ALIAS = "default";
    private static final int DEFAULT_PORT = 8181;
    private static final String DEFAULT_API_PATH = "/api";
    
    @Override
    public String name() {
        return "IDS API Configuration";
    }
    
    @Override
    public void initialize(ServiceExtensionContext context) {
        Monitor monitor = context.getMonitor();
    
        String contextAlias = DEFAULT_API_CONTEXT_ALIAS;
        String path = DEFAULT_API_PATH;
        int port = DEFAULT_PORT;
        
        Config config = context.getConfig(IDS_API_CONFIG);
        if (config.getEntries().isEmpty() || !config.getEntries().containsKey("path")
                || !config.getEntries().containsKey("port")) {
            monitor.warning(format("Settings [%s] and/or [%s] were not provided. IDS API will be"
                    + " registered in the default API context.", IDS_API_CONFIG + ".path",
                    IDS_API_CONFIG + ".path"));
        } else {
            contextAlias = IDS_API_CONTEXT_ALIAS;
            path = config.getString("path");
            port = config.getInteger("port");
        }
        
        monitor.info(format("IDS API will be available under [path=%s], [port=%s].", path, port));
        
        context.registerService(IdsApiConfiguration.class, new IdsApiConfiguration(contextAlias));
    }
    
}
