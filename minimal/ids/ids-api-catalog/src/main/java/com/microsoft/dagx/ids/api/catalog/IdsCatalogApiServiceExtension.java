/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.api.catalog;

import com.microsoft.dagx.ids.spi.daps.DapsService;
import com.microsoft.dagx.ids.spi.descriptor.IdsDescriptorService;
import com.microsoft.dagx.ids.spi.policy.IdsPolicyService;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.policy.PolicyRegistry;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import de.fraunhofer.iais.eis.DescriptionRequestMessageImpl;
import de.fraunhofer.iais.eis.DescriptionResponseMessageImpl;

import java.util.Set;

/**
 * Implements the IDS Controller REST API for catalog services.
 */
public class IdsCatalogApiServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public Set<String> requires() {
        return Set.of("ids.core");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerTypes(context);

        registerControllers(context);

        monitor.info("Initialized IDS Catalog API extension");
    }

    @Override
    public void start() {
        monitor.info("Started IDS Catalog API extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown IDS Catalog API extension");
    }

    private void registerTypes(ServiceExtensionContext context) {
        context.getTypeManager().registerTypes(DescriptionRequestMessageImpl.class);
        context.getTypeManager().registerTypes(DescriptionResponseMessageImpl.class);
    }

    private void registerControllers(ServiceExtensionContext context) {
        var webService = context.getService(WebService.class);
        var descriptorService = context.getService(IdsDescriptorService.class);
        var metadataStore = context.getService(MetadataStore.class);

        var dapService = context.getService(DapsService.class);
        var policyRegistry = context.getService(PolicyRegistry.class);
        var policyService = context.getService(IdsPolicyService.class);
        var queryEngine = new QueryEngineImpl(policyRegistry, policyService, metadataStore, monitor);


        webService.registerController(new DescriptionRequestController(descriptorService, metadataStore));
        webService.registerController(new CatalogQueryController(queryEngine, dapService));
    }


}
