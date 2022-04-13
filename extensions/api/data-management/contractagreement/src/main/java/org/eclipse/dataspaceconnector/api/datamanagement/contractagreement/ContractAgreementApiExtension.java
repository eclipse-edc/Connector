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

package org.eclipse.dataspaceconnector.api.datamanagement.contractagreement;

import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.api.datamanagement.contractagreement.service.ContractAgreementServiceImpl;
import org.eclipse.dataspaceconnector.api.datamanagement.contractagreement.transform.ContractAgreementToContractAgreementDtoTransformer;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import static java.util.Optional.ofNullable;

public class ContractAgreementApiExtension implements ServiceExtension {

    @Inject
    WebService webService;

    @Inject
    DataManagementApiConfiguration config;

    @Inject
    DtoTransformerRegistry transformerRegistry;

    @Inject(required = false)
    TransactionContext transactionContext;

    @Inject
    ContractNegotiationStore store;


    @Override
    public String name() {
        return "Data Management API: Contract Agreement";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new ContractAgreementToContractAgreementDtoTransformer());
        var monitor = context.getMonitor();

        var transactionContextImpl = ofNullable(transactionContext)
                .orElseGet(() -> {
                    monitor.warning("No TransactionContext registered, a no-op implementation will be used, not suitable for production environments");
                    return new NoopTransactionContext();
                });

        var service = new ContractAgreementServiceImpl(store, transactionContextImpl);
        var controller = new ContractAgreementApiController(monitor, service, transformerRegistry);
        webService.registerResource(config.getContextAlias(), controller);
    }
}