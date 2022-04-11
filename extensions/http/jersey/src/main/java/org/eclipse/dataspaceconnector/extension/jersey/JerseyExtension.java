/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.extension.jersey;

import org.eclipse.dataspaceconnector.extension.jetty.JettyService;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

@Provides({WebService.class})
@Requires({JettyService.class})
public class JerseyExtension implements ServiceExtension {
    private JerseyRestService jerseyRestService;

    @Override
    public String name() {
        return "Jersey Web Service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        TypeManager typeManager = context.getTypeManager();

        var jettyService = context.getService(JettyService.class);

        var corsConfiguration = CorsFilterConfiguration.from(context);

        jerseyRestService = new JerseyRestService(jettyService, typeManager, corsConfiguration, monitor);

        context.registerService(WebService.class, jerseyRestService);
    }

    @Override
    public void start() {
        jerseyRestService.start();
    }
}
