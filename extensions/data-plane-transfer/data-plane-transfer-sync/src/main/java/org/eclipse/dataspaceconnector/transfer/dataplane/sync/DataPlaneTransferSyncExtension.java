/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.sync;

import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyAccessManager;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.token.DataPlaneTransferTokenValidator;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.token.DataPlaneTransferValidationRulesRegistry;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.controller.DataPlaneTransferSyncApiController;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.rules.ContractValidationRule;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.rules.ExpirationDateValidationRule;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.flow.ProviderDataPlaneProxyDataFlowController;

public class DataPlaneTransferSyncExtension implements ServiceExtension {

    private static final String API_CONTEXT_ALIAS = "validation";

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Inject
    private WebService webService;

    @Inject
    private DataFlowManager dataFlowManager;

    @Inject
    private DataPlaneProxyAccessManager proxyManager;

    @Inject
    private DataEncrypter encrypter;

    @Inject
    private DataPlaneTransferValidationRulesRegistry validationRulesRegistry;

    @Inject
    private DataPlaneTransferTokenValidator tokenValidator;

    @Override
    public String name() {
        return "Data Plane Transfer Sync";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        validationRulesRegistry.addRule(new ContractValidationRule(contractNegotiationStore));
        validationRulesRegistry.addRule(new ExpirationDateValidationRule());

        webService.registerResource(API_CONTEXT_ALIAS, new DataPlaneTransferSyncApiController(context.getMonitor(), tokenValidator, encrypter));

        var flowController = new ProviderDataPlaneProxyDataFlowController(context.getConnectorId(), dispatcherRegistry, proxyManager);
        dataFlowManager.register(flowController);
    }
}
