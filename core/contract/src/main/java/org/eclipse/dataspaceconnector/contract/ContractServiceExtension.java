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
 *       Fraunhofer Institute for Software and Systems Engineering - add contract negotiation functionality
 *
 */

package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.contract.agent.ParticipantAgentServiceImpl;
import org.eclipse.dataspaceconnector.contract.negotiation.ConsumerContractNegotiationManagerImpl;
import org.eclipse.dataspaceconnector.contract.negotiation.ProviderContractNegotiationManagerImpl;
import org.eclipse.dataspaceconnector.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.dataspaceconnector.contract.offer.ContractDefinitionServiceImpl;
import org.eclipse.dataspaceconnector.contract.offer.ContractOfferServiceImpl;
import org.eclipse.dataspaceconnector.contract.policy.PolicyEngineImpl;
import org.eclipse.dataspaceconnector.contract.validation.ContractValidationServiceImpl;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.command.BoundedCommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandHandlerRegistry;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationWaitStrategy;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.contract.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.retry.ExponentialWaitStrategy;
import org.eclipse.dataspaceconnector.spi.system.CoreExtension;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;

@Provides({ContractOfferService.class, PolicyEngine.class, ParticipantAgentService.class, ContractValidationService.class,
        ConsumerContractNegotiationManager.class, ProviderContractNegotiationManager.class})
@CoreExtension
public class ContractServiceExtension implements ServiceExtension {

    private static final long DEFAULT_ITERATION_WAIT = 5000; // millis
    private Monitor monitor;
    private ConsumerContractNegotiationManagerImpl consumerNegotiationManager;
    private ProviderContractNegotiationManagerImpl providerNegotiationManager;

    @Inject
    private AssetIndex assetIndex;
    @Inject
    private ContractDefinitionStore contractDefinitionStore;
    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    @Inject
    private CommandHandlerRegistry commandHandlerRegistry;
    @Inject
    private ContractNegotiationStore store;

    @Override
    public String name() {
        return "Core Contract Service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerTypes(context);
        registerServices(context);
    }

    @Override
    public void start() {
        consumerNegotiationManager.start();
        providerNegotiationManager.start();
    }

    @Override
    public void shutdown() {
        if (consumerNegotiationManager != null) {
            consumerNegotiationManager.stop();
        }

        if (providerNegotiationManager != null) {
            providerNegotiationManager.stop();
        }
    }

    private void registerServices(ServiceExtensionContext context) {
        if (assetIndex == null) {
            monitor.warning("No AssetIndex registered. Register one to create Contract Offers.");
            assetIndex = new NullAssetIndex();
        }

        var agentService = new ParticipantAgentServiceImpl();
        context.registerService(ParticipantAgentService.class, agentService);

        var policyEngine = new PolicyEngineImpl();
        context.registerService(PolicyEngine.class, policyEngine);

        var definitionService = new ContractDefinitionServiceImpl(monitor, contractDefinitionStore, policyEngine);
        var contractOfferService = new ContractOfferServiceImpl(agentService, definitionService, assetIndex);
        context.registerService(ContractDefinitionService.class, definitionService);

        context.registerService(ContractOfferService.class, contractOfferService);

        var validationService = new ContractValidationServiceImpl(agentService, () -> definitionService, assetIndex);
        context.registerService(ContractValidationService.class, validationService);

        var waitStrategy = context.hasService(NegotiationWaitStrategy.class) ? context.getService(NegotiationWaitStrategy.class) : new ExponentialWaitStrategy(DEFAULT_ITERATION_WAIT);

        CommandQueue<ContractNegotiationCommand> commandQueue = new BoundedCommandQueue<>(10);
        CommandRunner<ContractNegotiationCommand> commandRunner = new CommandRunner<>(commandHandlerRegistry, monitor);

        var telemetry = context.getTelemetry();
        var observable = new ContractNegotiationObservableImpl();
        context.registerService(ContractNegotiationObservable.class, observable);

        consumerNegotiationManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .waitStrategy(waitStrategy)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .validationService(validationService)
                .commandQueue(commandQueue)
                .commandRunner(commandRunner)
                .observable(observable)
                .telemetry(telemetry)
                .store(store)
                .build();

        providerNegotiationManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .waitStrategy(waitStrategy)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .validationService(validationService)
                .commandQueue(commandQueue)
                .commandRunner(commandRunner)
                .observable(observable)
                .telemetry(telemetry)
                .store(store)
                .build();

        context.registerService(ConsumerContractNegotiationManager.class, consumerNegotiationManager);
        context.registerService(ProviderContractNegotiationManager.class, providerNegotiationManager);
    }

    private void registerTypes(ServiceExtensionContext context) {
        var typeManager = context.getTypeManager();
        typeManager.registerTypes(ContractNegotiation.class);
    }

}
