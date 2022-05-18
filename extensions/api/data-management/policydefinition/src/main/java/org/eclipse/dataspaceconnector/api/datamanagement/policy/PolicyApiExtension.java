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
import org.eclipse.dataspaceconnector.api.datamanagement.policy.service.PolicyServiceImpl;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import static java.util.Optional.ofNullable;

public class PolicyApiExtension implements ServiceExtension {

    @Inject
    DtoTransformerRegistry transformerRegistry;
    @Inject(required = false)
    TransactionContext transactionContext;
    @Inject
    private WebService webService;
    @Inject
    private DataManagementApiConfiguration configuration;
    @Inject
    private PolicyStore policyStore;
    @Inject
    private ContractDefinitionStore contractDefinitionStore;

    @Override
    public String name() {
        return "Data Management API: Policy";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var monitor = context.getMonitor();
        var transactionContextImpl = ofNullable(transactionContext)
                .orElseGet(() -> {
                    monitor.warning("No TransactionContext registered, a no-op implementation will be used, not suitable for production environments");
                    return new NoopTransactionContext();
                });
        var service = new PolicyServiceImpl(transactionContextImpl, policyStore, contractDefinitionStore);

        webService.registerResource(configuration.getContextAlias(), new PolicyApiController(monitor, service, transformerRegistry));
    }
}
