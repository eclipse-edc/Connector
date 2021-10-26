/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.ids.core;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.core.daps.DapsServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.descriptor.IdsDescriptorServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.message.DataRequestMessageSender;
import org.eclipse.dataspaceconnector.ids.core.message.IdsRemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.ids.core.message.QueryMessageSender;
import org.eclipse.dataspaceconnector.ids.core.policy.IdsPolicyServiceImpl;
import org.eclipse.dataspaceconnector.ids.core.version.ConnectorVersionProviderImpl;
import org.eclipse.dataspaceconnector.ids.spi.daps.DapsService;
import org.eclipse.dataspaceconnector.ids.spi.descriptor.IdsDescriptorService;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyService;
import org.eclipse.dataspaceconnector.ids.spi.version.ConnectorVersionProvider;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;

import java.util.Set;

/**
 * Implements the IDS Controller REST API.
 */
public class IdsCoreServiceExtension implements ServiceExtension {
    private static final String[] REQUIRES = {
            IdentityService.FEATURE, "dataspaceconnector:http-client", "dataspaceconnector:transferprocessstore"
    };

    private static final String[] PROVIDES = {
            "edc:ids:core"
    };

    private Monitor monitor;

    @Override
    public Set<String> provides() {
        return Set.of(PROVIDES);
    }

    @Override
    public Set<String> requires() {
        return Set.of(REQUIRES);
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        monitor = serviceExtensionContext.getMonitor();

        registerConnectorVersionProvider(serviceExtensionContext);
        registerOther(serviceExtensionContext);

        monitor.info("Initialized IDS Core extension");
    }

    private void registerOther(ServiceExtensionContext context) {
        var descriptorService = new IdsDescriptorServiceImpl();
        context.registerService(IdsDescriptorService.class, descriptorService);

        var identityService = context.getService(IdentityService.class);
        var connectorId = context.getConnectorId();
        var dapsService = new DapsServiceImpl(connectorId, identityService);
        context.registerService(DapsService.class, dapsService);

        var policyService = new IdsPolicyServiceImpl();
        context.registerService(IdsPolicyService.class, policyService);

        assembleIdsDispatcher(connectorId, context, identityService);
    }

    @Override
    public void start() {
        monitor.info("Started IDS Core extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown IDS Core extension");
    }

    private void registerConnectorVersionProvider(ServiceExtensionContext serviceExtensionContext) {
        ConnectorVersionProvider connectorVersionProvider = new ConnectorVersionProviderImpl();
        serviceExtensionContext.registerService(ConnectorVersionProvider.class, connectorVersionProvider);
    }

    /**
     * Assembles the IDS remote message dispatcher and its senders.
     */
    private void assembleIdsDispatcher(String connectorId, ServiceExtensionContext context, IdentityService identityService) {
        var processStore = context.getService(TransferProcessStore.class);
        var vault = context.getService(Vault.class);
        var httpClient = context.getService(OkHttpClient.class);

        var mapper = context.getTypeManager().getMapper();

        var monitor = context.getMonitor();

        var dispatcher = new IdsRemoteMessageDispatcher();

        dispatcher.register(new QueryMessageSender(connectorId, identityService, httpClient, mapper, monitor));
        dispatcher.register(new DataRequestMessageSender(connectorId, identityService, processStore, vault, httpClient, mapper, monitor));

        var registry = context.getService(RemoteMessageDispatcherRegistry.class);
        registry.register(dispatcher);
    }

}
