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
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.token.TokenValidationServiceImpl;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.controller.DataPlaneTransferSyncApiController;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.resolver.SelfPublicKeyResolver;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.rules.ContractValidationRule;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.rules.ExpirationDateValidationRule;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.flow.HttpProviderProxyDataFlowController;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.provision.HttpProviderProxyProvisionedResource;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.provision.HttpProviderProxyProvisioner;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.provision.HttpProviderProxyResourceDefinition;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.provision.HttpProviderProxyResourceGenerator;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.provision.HttpProviderProxyStatusChecker;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.schema.HttpProxySchema;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.transformer.ProxyEndpointDataReferenceTransformer;

import java.util.Arrays;

import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfiguration.API_CONTEXT_ALIAS;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfiguration.DATA_PLANE_PUBLIC_API_ENDPOINT;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfiguration.DATA_PLANE_PUBLIC_API_TOKEN_VALIDITY_SECONDS;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfiguration.DEFAULT_DATA_PLANE_PUBLIC_API_TOKEN_VALIDITY_SECONDS;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfiguration.PUBLIC_KEY_ALIAS;

@Provides({EndpointDataReferenceTransformer.class})
public class DataPlaneTransferSyncExtension implements ServiceExtension {

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Inject
    private TokenGenerationService tokenGenerationService;

    @Inject
    private DataAddressResolver dataAddressResolver;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Inject
    private DataEncrypter dataEncrypter;

    @Inject
    private ResourceManifestGenerator resourceManifestGenerator;

    @Inject
    private StatusCheckerRegistry statusCheckerRegistry;

    @Inject
    private WebService webService;

    @Inject
    private ProvisionManager provisionManager;

    @Inject
    private DataFlowManager dataFlowManager;

    @Override
    public String name() {
        return "Data Plane Transfer Sync";
    }

    /**
     * Register Api that is called by data plane in order to validate and decode tokens.
     */
    @Override
    public void initialize(ServiceExtensionContext context) {
        // api part
        var validationRules = Arrays.asList(new ContractValidationRule(contractNegotiationStore), new ExpirationDateValidationRule());
        var resolver = createResolver(context);
        var tokenValidationService = new TokenValidationServiceImpl(resolver, validationRules);
        webService.registerResource(API_CONTEXT_ALIAS, new DataPlaneTransferSyncApiController(context.getMonitor(), tokenValidationService, dataEncrypter));

        // provider part
        resourceManifestGenerator.registerProviderGenerator(new HttpProviderProxyResourceGenerator());
        statusCheckerRegistry.register(HttpProxySchema.TYPE, new HttpProviderProxyStatusChecker());

        registerProvisioner(context);
        dataFlowManager.register(new HttpProviderProxyDataFlowController(context.getConnectorId(), dispatcherRegistry));
        context.getTypeManager().registerTypes(HttpProviderProxyProvisionedResource.class, HttpProviderProxyResourceDefinition.class);

        // consumer part
        registerTransformer(context);
    }

    /**
     * Resolve public key from the Vault.
     */
    private static PublicKeyResolver createResolver(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);
        var publicKeyAlias = context.getConfig().getString(PUBLIC_KEY_ALIAS);
        return new SelfPublicKeyResolver(vault, publicKeyAlias);
    }

    /**
     * Register {@link EndpointDataReferenceTransformer} allowing to use consumer data plane as proxy.
     */
    private void registerTransformer(ServiceExtensionContext context) {
        var endpoint = context.getConfig().getString(DATA_PLANE_PUBLIC_API_ENDPOINT);
        var consumerProxyTransformer = new ProxyEndpointDataReferenceTransformer(tokenGenerationService, dataEncrypter, endpoint, context.getTypeManager());
        context.registerService(EndpointDataReferenceTransformer.class, consumerProxyTransformer);
    }

    /**
     * Register provider proxy provisioner serving data.
     */
    private void registerProvisioner(ServiceExtensionContext context) {
        var endpoint = context.getConfig().getString(DATA_PLANE_PUBLIC_API_ENDPOINT);
        var tokenValidity = context.getSetting(DATA_PLANE_PUBLIC_API_TOKEN_VALIDITY_SECONDS, DEFAULT_DATA_PLANE_PUBLIC_API_TOKEN_VALIDITY_SECONDS);
        var provisioner = new HttpProviderProxyProvisioner(endpoint, dataAddressResolver, dataEncrypter, tokenGenerationService, tokenValidity, context.getTypeManager());
        provisionManager.register(provisioner);
    }
}
