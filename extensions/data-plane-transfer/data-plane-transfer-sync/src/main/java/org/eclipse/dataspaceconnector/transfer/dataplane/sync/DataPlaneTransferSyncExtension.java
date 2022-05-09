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

import org.eclipse.dataspaceconnector.common.token.TokenValidationRulesRegistryImpl;
import org.eclipse.dataspaceconnector.common.token.TokenValidationServiceImpl;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreator;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.controller.DataPlaneTransferSyncApiController;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.resolver.SelfPublicKeyResolver;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.rules.ContractValidationRule;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.rules.ExpirationDateValidationRule;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.flow.ProviderDataPlaneProxyDataFlowController;

@Provides({EndpointDataReferenceTransformer.class})
public class DataPlaneTransferSyncExtension implements ServiceExtension {

    private static final String API_CONTEXT_ALIAS = "validation";

    @EdcSetting
    private static final String PUBLIC_KEY_ALIAS = "edc.public.key.alias";

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Inject
    private WebService webService;

    @Inject
    private DataFlowManager dataFlowManager;

    @Inject
    private DataPlaneTransferProxyCreator proxyManager;

    @Inject
    private DataEncrypter encrypter;

    @Override
    public String name() {
        return "Data Plane Transfer Sync";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerValidationApi(context);

        var flowController = new ProviderDataPlaneProxyDataFlowController(context.getConnectorId(), dispatcherRegistry, proxyManager);
        dataFlowManager.register(flowController);
    }

    /**
     * Register Api that is called by data plane in order to validate and decode tokens.
     */
    private void registerValidationApi(ServiceExtensionContext context) {
        var validationRulesRegistry = new TokenValidationRulesRegistryImpl();
        validationRulesRegistry.addRule(new ContractValidationRule(contractNegotiationStore));
        validationRulesRegistry.addRule(new ExpirationDateValidationRule());
        var resolver = createResolver(context);
        var tokenValidationService = new TokenValidationServiceImpl(resolver, validationRulesRegistry);
        webService.registerResource(API_CONTEXT_ALIAS, new DataPlaneTransferSyncApiController(context.getMonitor(), tokenValidationService, encrypter));
    }

    /**
     * Resolve public key from the Vault.
     */
    private static PublicKeyResolver createResolver(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);
        var publicKeyAlias = context.getConfig().getString(PUBLIC_KEY_ALIAS);
        return new SelfPublicKeyResolver(vault, publicKeyAlias);
    }
}
