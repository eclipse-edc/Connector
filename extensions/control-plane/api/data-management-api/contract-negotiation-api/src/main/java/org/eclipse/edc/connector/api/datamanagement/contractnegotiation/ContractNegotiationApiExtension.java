/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.datamanagement.contractnegotiation;

import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.edc.connector.api.datamanagement.contractnegotiation.transform.ContractAgreementToContractAgreementDtoTransformer;
import org.eclipse.edc.connector.api.datamanagement.contractnegotiation.transform.ContractNegotiationToContractNegotiationDtoTransformer;
import org.eclipse.edc.connector.api.datamanagement.contractnegotiation.transform.NegotiationInitiateRequestDtoToDataRequestTransformer;
import org.eclipse.edc.connector.controlplane.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

@Extension(value = ContractNegotiationApiExtension.NAME)
public class ContractNegotiationApiExtension implements ServiceExtension {

    public static final String NAME = "Data Management API: Contract Negotiation";
    @Inject
    private WebService webService;

    @Inject
    private DataManagementApiConfiguration config;

    @Inject
    private DtoTransformerRegistry transformerRegistry;

    @Inject
    private ContractNegotiationService service;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new ContractNegotiationToContractNegotiationDtoTransformer());
        transformerRegistry.register(new ContractAgreementToContractAgreementDtoTransformer());
        transformerRegistry.register(new NegotiationInitiateRequestDtoToDataRequestTransformer());

        var monitor = context.getMonitor();

        var controller = new ContractNegotiationApiController(monitor, service, transformerRegistry);
        webService.registerResource(config.getContextAlias(), controller);
    }
}
