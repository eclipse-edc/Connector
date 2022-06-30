/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       Microsoft Corporation - improvements
 *
 */

package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.common.statemachine.retry.EntitySendRetryManager;
import org.eclipse.dataspaceconnector.common.statemachine.retry.SendRetryManager;
import org.eclipse.dataspaceconnector.contract.negotiation.ConsumerContractNegotiationManagerImpl;
import org.eclipse.dataspaceconnector.contract.negotiation.ProviderContractNegotiationManagerImpl;
import org.eclipse.dataspaceconnector.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.dataspaceconnector.contract.offer.ContractDefinitionServiceImpl;
import org.eclipse.dataspaceconnector.contract.offer.ContractOfferServiceImpl;
import org.eclipse.dataspaceconnector.contract.policy.PolicyArchiveImpl;
import org.eclipse.dataspaceconnector.contract.validation.ContractValidationServiceImpl;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.command.BoundedCommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandHandlerRegistry;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationWaitStrategy;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.entity.StatefulEntity;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyArchive;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.retry.ExponentialWaitStrategy;
import org.eclipse.dataspaceconnector.spi.system.CoreExtension;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;

@Provides({
        ContractOfferService.class, ContractValidationService.class, ConsumerContractNegotiationManager.class,
        PolicyArchive.class, ProviderContractNegotiationManager.class, ContractNegotiationObservable.class
})
@CoreExtension
public class ContractServiceExtension implements ServiceExtension {

    private static final long DEFAULT_ITERATION_WAIT = 5000; // millis
    @EdcSetting
    private static final String NEGOTIATION_CONSUMER_STATE_MACHINE_BATCH_SIZE = "edc.negotiation.consumer.state-machine.batch-size";
    @EdcSetting
    private static final String NEGOTIATION_PROVIDER_STATE_MACHINE_BATCH_SIZE = "edc.negotiation.provider.state-machine.batch-size";
    @EdcSetting
    private static final String NEGOTIATION_CONSUMER_SEND_RETRY_LIMIT = "edc.negotiation.consumer.send.retry.limit";
    @EdcSetting
    private static final String NEGOTIATION_PROVIDER_SEND_RETRY_LIMIT = "edc.negotiation.provider.send.retry.limit";
    @EdcSetting
    private static final String NEGOTIATION_CONSUMER_SEND_RETRY_BASE_DELAY_MS = "edc.negotiation.consumer.send.retry.base-delay.ms";
    @EdcSetting
    private static final String NEGOTIATION_PROVIDER_SEND_RETRY_BASE_DELAY_MS = "edc.negotiation.provider.send.retry.base-delay.ms";

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

    @Inject
    private ParticipantAgentService agentService;

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private PolicyDefinitionStore policyStore;

    @Inject
    private Monitor monitor;

    @Inject
    private Telemetry telemetry;

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return "Core Contract Service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
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
        var definitionService = new ContractDefinitionServiceImpl(monitor, contractDefinitionStore, policyEngine, policyStore);
        context.registerService(ContractDefinitionService.class, definitionService);

        var contractOfferService = new ContractOfferServiceImpl(agentService, definitionService, assetIndex, policyStore);
        context.registerService(ContractOfferService.class, contractOfferService);

        var validationService = new ContractValidationServiceImpl(agentService, definitionService, assetIndex, policyStore, clock);
        context.registerService(ContractValidationService.class, validationService);

        var waitStrategy = context.hasService(NegotiationWaitStrategy.class) ? context.getService(NegotiationWaitStrategy.class) : new ExponentialWaitStrategy(DEFAULT_ITERATION_WAIT);

        CommandQueue<ContractNegotiationCommand> commandQueue = new BoundedCommandQueue<>(10);
        CommandRunner<ContractNegotiationCommand> commandRunner = new CommandRunner<>(commandHandlerRegistry, monitor);

        var observable = new ContractNegotiationObservableImpl();
        context.registerService(ContractNegotiationObservable.class, observable);
        context.registerService(PolicyArchive.class, new PolicyArchiveImpl(store));

        consumerNegotiationManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .waitStrategy(waitStrategy)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .validationService(validationService)
                .commandQueue(commandQueue)
                .commandRunner(commandRunner)
                .observable(observable)
                .clock(clock)
                .telemetry(telemetry)
                .executorInstrumentation(context.getService(ExecutorInstrumentation.class))
                .store(store)
                .policyStore(policyStore)
                .batchSize(context.getSetting(NEGOTIATION_CONSUMER_STATE_MACHINE_BATCH_SIZE, 5))
                .sendRetryManager(consumerSendRetryManager(context))
                .build();

        providerNegotiationManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .waitStrategy(waitStrategy)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .validationService(validationService)
                .commandQueue(commandQueue)
                .commandRunner(commandRunner)
                .observable(observable)
                .clock(clock)
                .telemetry(telemetry)
                .executorInstrumentation(context.getService(ExecutorInstrumentation.class))
                .store(store)
                .policyStore(policyStore)
                .batchSize(context.getSetting(NEGOTIATION_PROVIDER_STATE_MACHINE_BATCH_SIZE, 5))
                .sendRetryManager(providerSendRetryManager(context))
                .build();

        context.registerService(ConsumerContractNegotiationManager.class, consumerNegotiationManager);
        context.registerService(ProviderContractNegotiationManager.class, providerNegotiationManager);
    }

    private SendRetryManager<StatefulEntity> providerSendRetryManager(ServiceExtensionContext context) {
        var retryLimit = context.getSetting(NEGOTIATION_PROVIDER_SEND_RETRY_LIMIT, 7);
        var retryBaseDelay = context.getSetting(NEGOTIATION_PROVIDER_SEND_RETRY_BASE_DELAY_MS, 100L);
        return new EntitySendRetryManager(monitor, () -> new ExponentialWaitStrategy(retryBaseDelay), clock, retryLimit);
    }

    @NotNull
    private EntitySendRetryManager consumerSendRetryManager(ServiceExtensionContext context) {
        var retryLimit = context.getSetting(NEGOTIATION_CONSUMER_SEND_RETRY_LIMIT, 7);
        var retryBaseDelay = context.getSetting(NEGOTIATION_CONSUMER_SEND_RETRY_BASE_DELAY_MS, 100L);
        return new EntitySendRetryManager(monitor, () -> new ExponentialWaitStrategy(retryBaseDelay), clock, retryLimit);
    }

    private void registerTypes(ServiceExtensionContext context) {
        var typeManager = context.getTypeManager();
        typeManager.registerTypes(ContractNegotiation.class);
    }

}
