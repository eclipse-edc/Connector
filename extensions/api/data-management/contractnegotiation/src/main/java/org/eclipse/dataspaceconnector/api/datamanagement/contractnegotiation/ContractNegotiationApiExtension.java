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

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation;

import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service.ContractNegotiationServiceImpl;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.transform.ContractAgreementToContractAgreementDtoTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.transform.ContractNegotiationToContractNegotiationDtoTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.transform.NegotiationInitiateRequestDtoToDataRequestTransformer;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

public class ContractNegotiationApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;

    @Inject
    private DataManagementApiConfiguration config;

    @Inject
    private DtoTransformerRegistry transformerRegistry;

    @Inject
    private ContractNegotiationStore store;

    @Inject
    private ConsumerContractNegotiationManager manager;

    @Inject
    private TransactionContext transactionContext;

    @Override
    public String name() {
        return "Data Management API: Contract Negotiation";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new ContractNegotiationToContractNegotiationDtoTransformer());
        transformerRegistry.register(new ContractAgreementToContractAgreementDtoTransformer());
        transformerRegistry.register(new NegotiationInitiateRequestDtoToDataRequestTransformer());

        var monitor = context.getMonitor();

        var service = new ContractNegotiationServiceImpl(store, manager, transactionContext);
        var controller = new ContractNegotiationApiController(monitor, service, transformerRegistry);
        webService.registerResource(config.getContextAlias(), controller);
    }
}
