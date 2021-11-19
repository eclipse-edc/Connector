/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.contract.agent.ParticipantAgentServiceImpl;
import org.eclipse.dataspaceconnector.contract.offer.ContractOfferServiceImpl;
import org.eclipse.dataspaceconnector.contract.offer.NullContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;
import java.util.function.Supplier;

public class ContractServiceExtension implements ServiceExtension {
    private static final String NAME = "Core Contract Service Extension";
    private static final Set<String> PROVIDES = Set.of("edc:core:contract");

    private Monitor monitor;

    @Override
    public final Set<String> provides() {
        return PROVIDES;
    }

    @Override
    public final Set<String> requires() {
        return Set.of(AssetIndex.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        monitor = serviceExtensionContext.getMonitor();

        registerServices(serviceExtensionContext);

        monitor.info(String.format("Initialized %s", NAME));
    }

    @Override
    public void start() {
        monitor.info(String.format("Started %s", NAME));
    }

    @Override
    public void shutdown() {
        monitor.info(String.format("Shutdown %s", NAME));
    }

    private void registerServices(ServiceExtensionContext context) {

        var assetIndex = context.getService(AssetIndex.class, true);
        if (assetIndex == null) {
            monitor.warning("No AssetIndex registered. Register one to create Contract Offers.");
            assetIndex = new NullAssetIndex();
        }

        var agentService = new ParticipantAgentServiceImpl();
        context.registerService(ParticipantAgentService.class, agentService);

        // Lazily load the contract offer framework into this core module since the implementation is provided by an extension that will require other core services
        var cofSupplier = new CofSupplier(context, monitor);
        var contractOfferService = new ContractOfferServiceImpl(agentService, cofSupplier, assetIndex);

        // Register the created contract offer service with the service extension context.
        context.registerService(ContractOfferService.class, contractOfferService);

    }

    private static class CofSupplier implements Supplier<ContractDefinitionService> {
        private final ServiceExtensionContext context;
        private final Monitor monitor;
        private ContractDefinitionService cachedFramework;

        public CofSupplier(ServiceExtensionContext context, Monitor monitor) {
            this.context = context;
            this.monitor = monitor;
        }

        @Override
        public ContractDefinitionService get() {
            // Note this implementation is purposely not synchronized or reliant on volatiles in favor of runtime lookup performance; at worst, multiple copies of a null
            // contract offer framework will be instantiated, which has no operational impact.
            if (cachedFramework != null) {
                cachedFramework = context.getService(ContractDefinitionService.class, true);
                if (cachedFramework == null) {
                    monitor.warning("No ContractDefinitionService registered. Register one to create Contract Offers.");
                    cachedFramework = new NullContractDefinitionService();
                }
            }
            return cachedFramework;

        }
    }
}
