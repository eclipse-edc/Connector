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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.service.ContractDefinitionEventListener;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.service.ContractDefinitionService;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.service.ContractDefinitionServiceImpl;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform.ContractDefinitionRequestDtoToContractDefinitionTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform.ContractDefinitionToContractDefinitionResponseDtoTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform.CriterionDtoToCriterionTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform.CriterionToCriterionDtoTransformer;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.definition.observe.ContractDefinitionObservableImpl;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import java.time.Clock;

@Provides(ContractDefinitionService.class)
public class ContractDefinitionApiExtension implements ServiceExtension {
    @Inject
    WebService webService;

    @Inject
    DataManagementApiConfiguration config;

    @Inject
    DtoTransformerRegistry transformerRegistry;

    @Inject
    ContractDefinitionStore contractDefinitionStore;

    @Inject
    TransactionContext transactionContext;

    @Inject
    Clock clock;

    @Inject
    EventRouter eventRouter;

    @Override
    public String name() {
        return "Data Management API: Contract Definition";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new CriterionToCriterionDtoTransformer());
        transformerRegistry.register(new CriterionDtoToCriterionTransformer());
        transformerRegistry.register(new ContractDefinitionToContractDefinitionResponseDtoTransformer());
        transformerRegistry.register(new ContractDefinitionRequestDtoToContractDefinitionTransformer());

        var monitor = context.getMonitor();

        var contractDefinitionObservable = new ContractDefinitionObservableImpl();
        contractDefinitionObservable.registerListener(new ContractDefinitionEventListener(clock, eventRouter));

        var service = new ContractDefinitionServiceImpl(contractDefinitionStore, transactionContext, contractDefinitionObservable);
        context.registerService(ContractDefinitionService.class, service);

        webService.registerResource(config.getContextAlias(), new ContractDefinitionApiController(monitor, service, transformerRegistry));
    }
}
