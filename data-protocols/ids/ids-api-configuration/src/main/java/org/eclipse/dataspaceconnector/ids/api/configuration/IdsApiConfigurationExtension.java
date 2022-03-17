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

import org.eclipse.dataspaceconnector.extension.jetty.JettyService;
import org.eclipse.dataspaceconnector.extension.jetty.PortMapping;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import static java.lang.String.format;

/**
 * Provides configuration information for IDS API endpoints to other extensions.
 */
@Provides(IdsApiConfiguration.class)
public class IdsApiConfigurationExtension implements ServiceExtension {
    
    public static final String IDS_API_CONFIG = "web.http.ids";
    private static final String IDS_API_CONTEXT_ALIAS = "ids";
    
    public static final int DEFAULT_IDS_PORT = 8282;
    public static final String DEFAULT_IDS_API_PATH = "/api/v1/ids";
    
    @Inject
    private JettyService jettyService;
    
    @Override
    public String name() {
        return "IDS API Configuration";
    }
    
    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
    
        var contextAlias = IDS_API_CONTEXT_ALIAS;
        var path = DEFAULT_IDS_API_PATH;
        var port = DEFAULT_IDS_PORT;
        
        var config = context.getConfig(IDS_API_CONFIG);
        if (config.getEntries().isEmpty()) {
            monitor.warning(format("Settings for [%s] and/or [%s] were not provided. Using default" +
                    " value(s) instead.", IDS_API_CONFIG + ".path", IDS_API_CONFIG + ".path"));
            jettyService.addPortMapping(new PortMapping(contextAlias, port, path));
        } else {
            path = config.getString("path", path);
            port = config.getInteger("port", port);
        }
        
        monitor.info(format("IDS API will be available at [path=%s], [port=%s].", path, port));
        
        context.registerService(IdsApiConfiguration.class, new IdsApiConfiguration(contextAlias, path));
    }
    
}
