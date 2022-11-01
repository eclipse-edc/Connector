/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.edc.connector.api.datamanagement.contractagreement;

import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.edc.connector.api.datamanagement.contractagreement.service.ContractAgreementServiceImpl;
import org.eclipse.edc.connector.api.datamanagement.contractagreement.transform.ContractAgreementToContractAgreementDtoTransformer;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.web.spi.WebService;

@Extension(value = ContractAgreementApiExtension.NAME)
public class ContractAgreementApiExtension implements ServiceExtension {

    public static final String NAME = "Data Management API: Contract Agreement";
    @Inject
    WebService webService;

    @Inject
    DataManagementApiConfiguration config;

    @Inject
    DtoTransformerRegistry transformerRegistry;

    @Inject
    TransactionContext transactionContext;

    @Inject
    ContractNegotiationStore store;


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new ContractAgreementToContractAgreementDtoTransformer());
        var monitor = context.getMonitor();

        var service = new ContractAgreementServiceImpl(store, transactionContext);
        var controller = new ContractAgreementApiController(monitor, service, transformerRegistry);
        webService.registerResource(config.getContextAlias(), controller);
    }
}
