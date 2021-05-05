/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.api.transfer;

import com.microsoft.dagx.ids.spi.daps.DapsService;
import com.microsoft.dagx.ids.spi.policy.IdsPolicyService;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.policy.PolicyRegistry;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;

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
