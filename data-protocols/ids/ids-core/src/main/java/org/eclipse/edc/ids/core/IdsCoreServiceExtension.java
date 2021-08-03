/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.ids.core;

import org.eclipse.edc.ids.core.daps.DapsServiceImpl;
import org.eclipse.edc.ids.core.descriptor.IdsDescriptorServiceImpl;
import org.eclipse.edc.ids.core.message.DataRequestMessageSender;
import org.eclipse.edc.ids.core.message.IdsRemoteMessageDispatcher;
import org.eclipse.edc.ids.core.message.QueryMessageSender;
import org.eclipse.edc.ids.core.policy.IdsPolicyServiceImpl;
import org.eclipse.edc.ids.spi.daps.DapsService;
import org.eclipse.edc.ids.spi.descriptor.IdsDescriptorService;
import org.eclipse.edc.ids.spi.policy.IdsPolicyService;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.transfer.store.TransferProcessStore;
import okhttp3.OkHttpClient;

import java.util.Set;

import static org.eclipse.edc.common.settings.SettingsHelper.getConnectorId;

/**
 * Implements the IDS Controller REST API.
 */
public class IdsCoreServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public Set<String> provides() {
        return Set.of("ids.core");
    }

    @Override
    public Set<String> requires() {
        return Set.of("iam", "edc:http-client");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        var descriptorService = new IdsDescriptorServiceImpl();
        context.registerService(IdsDescriptorService.class, descriptorService);

        var identityService = context.getService(IdentityService.class);
        var connectorName = getConnectorId(context);
        var dapsService = new DapsServiceImpl(connectorName, identityService);
        context.registerService(DapsService.class, dapsService);

        var policyService = new IdsPolicyServiceImpl();
        context.registerService(IdsPolicyService.class, policyService);

        assembleIdsDispatcher(connectorName, context, identityService);

        monitor.info("Initialized IDS Core extension");
    }

    @Override
    public void start() {
        monitor.info("Started IDS Core extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown IDS Core extension");
    }

    /**
     * Assembles the IDS remote message dispatcher and its senders.
     */
    private void assembleIdsDispatcher(String connectorName, ServiceExtensionContext context, IdentityService identityService) {
        var processStore = context.getService(TransferProcessStore.class);
        var vault = context.getService(Vault.class);
        var httpClient = context.getService(OkHttpClient.class);

        var mapper = context.getTypeManager().getMapper();

        var monitor = context.getMonitor();

        var dispatcher = new IdsRemoteMessageDispatcher();

        dispatcher.register(new QueryMessageSender(connectorName, identityService, httpClient, mapper, monitor));
        dispatcher.register(new DataRequestMessageSender(connectorName, identityService, processStore, vault, httpClient, mapper, monitor));

        var registry = context.getService(RemoteMessageDispatcherRegistry.class);
        registry.register(dispatcher);
    }

}
