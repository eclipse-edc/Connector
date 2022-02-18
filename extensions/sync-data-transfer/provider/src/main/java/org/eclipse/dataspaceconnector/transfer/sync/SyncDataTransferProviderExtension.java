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

package org.eclipse.dataspaceconnector.transfer.sync;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.transfer.sync.flow.HttpProviderProxyDataFlowController;
import org.eclipse.dataspaceconnector.transfer.sync.provision.HttpProviderProxyProvisionedResource;
import org.eclipse.dataspaceconnector.transfer.sync.provision.HttpProviderProxyProvisioner;
import org.eclipse.dataspaceconnector.transfer.sync.provision.HttpProviderProxyResourceDefinition;
import org.eclipse.dataspaceconnector.transfer.sync.provision.HttpProviderProxyResourceGenerator;
import org.eclipse.dataspaceconnector.transfer.sync.provision.HttpProviderProxyStatusChecker;

import java.util.concurrent.TimeUnit;

public class SyncDataTransferProviderExtension implements ServiceExtension {

    @EdcSetting
    private static final String PROVIDER_DATA_API_ADDRESS = "edc.transfer.sync.data-api.address";

    @EdcSetting
    private static final String PROVIDER_DATA_PROXY_TOKEN_VALIDITY = "edc.transfer.sync.token.validity";
    private static final long DEFAULT_PROVIDER_DATA_PROXY_TOKEN_VALIDITY = TimeUnit.MINUTES.toSeconds(10);

    @Inject
    private TokenGenerationService tokenGenerationService;

    @Inject
    private DataAddressResolver dataAddressResolver;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Override

    public String name() {
        return "Sync Data Transfer Provisioner";
    }

    /**
     * Registers {@link HttpProviderProxyProvisioner} and {@link HttpProviderProxyDataFlowController}.
     * Especially the provisioner is in charge of building a proxy entry composed of an endpoint serving the token and an eventual security token.
     * This proxy is then conveyed by the {@link HttpProviderProxyDataFlowController} to the consumer control plane as an {@link org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference}.
     */
    @Override
    public void initialize(ServiceExtensionContext context) {
        var manifestGenerator = context.getService(ResourceManifestGenerator.class);
        manifestGenerator.registerProviderGenerator(new HttpProviderProxyResourceGenerator());

        var statusCheckerReg = context.getService(StatusCheckerRegistry.class);
        statusCheckerReg.register("HttpData", new HttpProviderProxyStatusChecker());

        registerProvisioner(context);
        registerDataFlowController(context);

        context.getTypeManager().registerTypes(HttpProviderProxyProvisionedResource.class, HttpProviderProxyResourceDefinition.class);
    }

    /**
     * Register provider proxy provisioner serving data.
     */
    private void registerProvisioner(ServiceExtensionContext context) {
        var provisionManager = context.getService(ProvisionManager.class);
        var endpoint = context.getSetting(PROVIDER_DATA_API_ADDRESS, null);
        if (endpoint == null) {
            throw new EdcException(String.format("Missing mandatory setting `%s`", PROVIDER_DATA_API_ADDRESS));
        }
        var tokenValidity = context.getSetting(PROVIDER_DATA_PROXY_TOKEN_VALIDITY, DEFAULT_PROVIDER_DATA_PROXY_TOKEN_VALIDITY);
        var provisioner = new HttpProviderProxyProvisioner(endpoint, dataAddressResolver, tokenGenerationService, tokenValidity, context.getTypeManager());
        provisionManager.register(provisioner);
    }

    /**
     * Register data flow controller in charge of sending to the consumer EDC the
     * {@link org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference} which describes the endpoint serving the data.
     * Note that default protocol for exchange is `ids-multipart`.
     */
    private void registerDataFlowController(ServiceExtensionContext context) {
        var dataFlowManager = context.getService(DataFlowManager.class);
        dataFlowManager.register(new HttpProviderProxyDataFlowController(context.getConnectorId(), dispatcherRegistry));
    }
}
