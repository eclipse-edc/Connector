/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dsp.controller;

import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

/**
 * Bootstraps the TCK web hook.
 */
public class TckControllerExtension implements ServiceExtension {
    private static final String NAME = "DSP TCK Controller";
    private static final String PROTOCOL = "tck";
    private static final String PATH = "/tck";
    private static final int PORT = 8687;
    @Inject
    private PortMappingRegistry mappingRegistry;
    @Inject
    private WebService webService;
    @Inject
    private WebServer webServer;
    @Inject
    private ContractNegotiationService negotiationService;
    @Inject
    private TransferProcessService transferProcessService;
    @Inject
    private Monitor monitor;

    @Configuration
    private TckWebhookApiConfiguration apiConfiguration;

    @Inject
    private SingleParticipantContextSupplier participantContextSupplier;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        mappingRegistry.register(new PortMapping(PROTOCOL, apiConfiguration.port(), apiConfiguration.path()));
        webService.registerResource(PROTOCOL, new TckWebhookController(monitor, negotiationService, transferProcessService, participantContextSupplier));
    }

    @Settings
    record TckWebhookApiConfiguration(
            @Setting(key = "web.http." + PROTOCOL + ".port", description = "Port for " + PROTOCOL + " api context", defaultValue = PORT + "")
            int port,
            @Setting(key = "web.http." + PROTOCOL + ".path", description = "Path for " + PROTOCOL + " api context", defaultValue = PATH)
            String path
    ) {

    }
}
