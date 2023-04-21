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

package org.eclipse.edc.protocol.dsp.negotiation.api;

import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.api.configuration.DspApiConfiguration;
import org.eclipse.edc.protocol.dsp.negotiation.api.controller.DspNegotiationController;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;

/**
 * Creates and registers the controller for dataspace protocol negotiation requests.
 */
@Extension(value = DspNegotiationApiExtension.NAME)
public class DspNegotiationApiExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Negotiation Api Extension";

    @Inject
    private WebService webService;

    @Inject
    private TypeManager typeManager;

    @Inject
    private IdentityService identityService;

    @Inject
    private DspApiConfiguration apiConfiguration;

    @Inject
    private JsonLdTransformerRegistry transformerRegistry;

    @Inject
    private Monitor monitor;

    @Inject
    private ContractNegotiationService negotiationService;

    @Inject
    private ContractNegotiationProtocolService protocolService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var callbackAddress = apiConfiguration.getDspCallbackAddress();
        var controller = new DspNegotiationController(monitor, typeManager, callbackAddress, identityService, transformerRegistry, negotiationService, protocolService);

        webService.registerResource(apiConfiguration.getContextAlias(), controller);
    }
}
