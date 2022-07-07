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
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.policy;

import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.service.PolicyDefinitionEventListener;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.service.PolicyDefinitionService;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.service.PolicyDefinitionServiceImpl;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.observe.policydefinition.PolicyDefinitionObservableImpl;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import java.time.Clock;

@Provides(PolicyDefinitionService.class)
public class PolicyDefinitionApiExtension implements ServiceExtension {

    @Inject
    private DtoTransformerRegistry transformerRegistry;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private WebService webService;
    @Inject
    private DataManagementApiConfiguration configuration;
    @Inject
    private PolicyDefinitionStore policyStore;
    @Inject
    private ContractDefinitionStore contractDefinitionStore;
    @Inject
    private EventRouter eventRouter;
    @Inject
    private Clock clock;

    @Override
    public String name() {
        return "Data Management API: Policy";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var policyDefinitionObservable = new PolicyDefinitionObservableImpl();
        policyDefinitionObservable.registerListener(new PolicyDefinitionEventListener(clock, eventRouter));

        var service = new PolicyDefinitionServiceImpl(transactionContext, policyStore, contractDefinitionStore, policyDefinitionObservable);
        context.registerService(PolicyDefinitionService.class, service);

        webService.registerResource(configuration.getContextAlias(), new PolicyDefinitionApiController(monitor, service, transformerRegistry));
    }
}
