/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dsp.controller;

import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

/**
 * Bootstraps the TCK web hook.
 */
public class TckControllerExtension implements ServiceExtension {
    private static final String NAME = "DSP TCK Controller";
    private static final String PROTOCOL = "tck";

    private static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey(PROTOCOL)
            .contextAlias("tck")
            .defaultPath("/tck")
            .defaultPort(8687)
            .useDefaultContext(false)
            .name("Tck API")
            .build();


    @Inject
    private WebServiceConfigurer configurator;

    @Inject
    private WebService webService;

    @Inject
    private WebServer webServer;

    @Inject
    private ContractNegotiationService negotiationService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var config = context.getConfig(PROTOCOL);
        configurator.configure(config, webServer, SETTINGS);
        webService.registerResource(PROTOCOL, new TckWebhookController(negotiationService));
    }
}
