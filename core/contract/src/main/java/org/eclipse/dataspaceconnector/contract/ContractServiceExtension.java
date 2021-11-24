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
import org.eclipse.dataspaceconnector.contract.offer.ContractDefinitionServiceImpl;
import org.eclipse.dataspaceconnector.contract.offer.ContractOfferServiceImpl;
import org.eclipse.dataspaceconnector.contract.policy.PolicyEngineImpl;
import org.eclipse.dataspaceconnector.contract.validation.ContractValidationServiceImpl;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.InMemoryContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.contract.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class ContractServiceExtension implements ServiceExtension {
    private static final String NAME = "Core Contract Service Extension";
    private static final Set<String> PROVIDES = Set.of("edc:core:contract");

    private Monitor monitor;
    private ServiceExtensionContext context;
    private ContractDefinitionServiceImpl definitionService;

    @Override
    public final Set<String> provides() {
        return PROVIDES;
    }

    @Override
    public final Set<String> requires() {
        return Set.of(AssetIndex.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        this.context = context;

        registerServices(context);

        monitor.info(String.format("Initialized %s", NAME));
    }

    @Override
    public void start() {
        // load the store in the start method so it can be overridden by an extension
        definitionService.initialize(context.getService(ContractDefinitionStore.class));
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

        var policyEngine = new PolicyEngineImpl();
        context.registerService(PolicyEngine.class, policyEngine);

        var definitionsStore = new InMemoryContractDefinitionStore();
        context.registerService(ContractDefinitionStore.class, definitionsStore);

        definitionService = new ContractDefinitionServiceImpl(policyEngine, monitor);
        var contractOfferService = new ContractOfferServiceImpl(agentService, definitionService, assetIndex);

        // Register the created contract offer service with the service extension context.
        context.registerService(ContractOfferService.class, contractOfferService);

        var validationService = new ContractValidationServiceImpl(agentService, () -> context.getService(ContractDefinitionService.class), assetIndex);
        context.registerService(ContractValidationService.class, validationService);
    }

}
