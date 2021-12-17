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

package org.eclipse.dataspaceconnector.web;

import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.web.rest.CorsFilterConfiguration;
import org.eclipse.dataspaceconnector.web.rest.JerseyRestService;
import org.eclipse.dataspaceconnector.web.transport.JettyService;

import java.util.Set;

import static org.eclipse.dataspaceconnector.spi.system.ServiceExtension.LoadPhase.PRIMORDIAL;

/**
 * Provides HTTP transport and REST binding services.
 * <br/>
 * TODO create keystore to support HTTPS
 */
public class WebServiceExtension implements ServiceExtension {
    private JettyService jettyService;
    private JerseyRestService jerseyRestService;

    @Override
    public String name() {
        return "Web Service";
    }

    @Override
    public Set<String> provides() {
        return Set.of("edc:webservice");
    }

    @Override
    public LoadPhase phase() {
        return PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        TypeManager typeManager = context.getTypeManager();

        jettyService = new JettyService(context::getSetting, monitor);
        context.registerService(JettyService.class, jettyService);

        var corsConfiguration = CorsFilterConfiguration.from(context);

        jerseyRestService = new JerseyRestService(jettyService, typeManager, corsConfiguration, monitor);

        context.registerService(WebService.class, jerseyRestService);
    }

    @Override
    public void start() {
        jerseyRestService.start();
        jettyService.start();
    }

    @Override
    public void shutdown() {
        if (jettyService != null) {
            jettyService.shutdown();
        }
    }


}
