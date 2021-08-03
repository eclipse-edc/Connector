/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.ids.api.transfer;

import org.eclipse.edc.ids.spi.daps.DapsService;
import org.eclipse.edc.ids.spi.policy.IdsPolicyService;
import org.eclipse.edc.spi.metadata.MetadataStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.policy.PolicyRegistry;
import org.eclipse.edc.spi.protocol.web.WebService;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.transfer.TransferProcessManager;

import java.util.Set;

/**
 * Implements the IDS Controller REST API for data transfer services.
 */
public class IdsTransferApiServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public Set<String> requires() {
        return Set.of("ids.core", "policy-registry");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerControllers(context);

        monitor.info("Initialized IDS Transfer API extension");
    }

    @Override
    public void start() {
        monitor.info("Started IDS Transfer API extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown IDS Transfer API extension");
    }

    private void registerControllers(ServiceExtensionContext context) {

        var webService = context.getService(WebService.class);

        var dapService = context.getService(DapsService.class);

        var transferManager = context.getService(TransferProcessManager.class);

        var metadataStore = context.getService(MetadataStore.class);

        var policyService = context.getService(IdsPolicyService.class);

        var monitor = context.getMonitor();

        var vault = context.getService(Vault.class);

        var policyRegistry = context.getService(PolicyRegistry.class);

        webService.registerController(new ArtifactRequestController(dapService, metadataStore, transferManager, policyService, policyRegistry, vault, monitor));
    }


}
