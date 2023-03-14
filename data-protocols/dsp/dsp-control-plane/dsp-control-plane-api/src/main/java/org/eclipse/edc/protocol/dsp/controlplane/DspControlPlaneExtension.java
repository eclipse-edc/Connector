/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.edc.protocol.dsp.controlplane;

import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.ProviderContractNegotiationManager;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.protocol.dsp.api.configuration.DspApiConfiguration;
import org.eclipse.edc.protocol.dsp.controlplane.controller.ContractNegotiationController;
import org.eclipse.edc.protocol.dsp.controlplane.controller.TransferProcessController;
import org.eclipse.edc.protocol.dsp.controlplane.service.DspContractNegotiationServiceImpl;
import org.eclipse.edc.protocol.dsp.controlplane.service.TransferProcessServiceImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;

@Extension(value = DspControlPlaneExtension.NAME)
public class DspControlPlaneExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol: Control Plane Extension";

    @Inject
    private ConsumerContractNegotiationManager consumerNegotiationManager;

    @Inject
    private ProviderContractNegotiationManager providerNegotiationManager;

    private ContractNegotiationService contractNegotiationService;

    @Inject
    private DspApiConfiguration config;

    @Inject
    private Monitor monitor;

    @Inject
    private WebService webService;

    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var negotiationService = new DspContractNegotiationServiceImpl(consumerNegotiationManager, providerNegotiationManager, contractNegotiationService);

        var contractNegotiationController = new ContractNegotiationController(monitor, negotiationService, typeManager);

        var transferProcessService = new TransferProcessServiceImpl();
        var transferProcessController = new TransferProcessController(monitor,transferProcessService, typeManager);

        webService.registerResource(config.getContextAlias(), transferProcessController);
        webService.registerResource(config.getContextAlias(), contractNegotiationController);
    }
}
