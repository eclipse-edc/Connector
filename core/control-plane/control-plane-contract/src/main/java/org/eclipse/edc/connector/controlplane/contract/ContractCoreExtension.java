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

package org.eclipse.edc.connector.controlplane.contract;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.listener.ContractNegotiationEventListener;
import org.eclipse.edc.connector.controlplane.contract.negotiation.ConsumerContractNegotiationManagerImpl;
import org.eclipse.edc.connector.controlplane.contract.negotiation.ProviderContractNegotiationManagerImpl;
import org.eclipse.edc.connector.controlplane.contract.policy.ContractExpiryFunction;
import org.eclipse.edc.connector.controlplane.contract.policy.PolicyEquality;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.NegotiationWaitStrategy;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ProviderContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.controlplane.contract.validation.ContractValidationServiceImpl;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.CoreExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;

import static org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext.CATALOG_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext.NEGOTIATION_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext.TRANSFER_SCOPE;
import static org.eclipse.edc.connector.controlplane.policy.contract.ContractExpiryCheck.CONTRACT_EXPIRY_EVALUATION_KEY;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_BATCH_SIZE;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_ITERATION_WAIT;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_SEND_RETRY_BASE_DELAY;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_SEND_RETRY_LIMIT;

@Provides({
        ContractValidationService.class, ConsumerContractNegotiationManager.class,
        ProviderContractNegotiationManager.class
})
@CoreExtension
@Extension(value = ContractCoreExtension.NAME)
public class ContractCoreExtension implements ServiceExtension {

    public static final String NAME = "Contract Core";

    @Setting(value = "the iteration wait time in milliseconds in the negotiation state machine. Default value " + DEFAULT_ITERATION_WAIT, type = "long")
    private static final String NEGOTIATION_STATE_MACHINE_ITERATION_WAIT_MILLIS = "edc.negotiation.state-machine.iteration-wait-millis";

    @Setting(value = "the batch size in the consumer negotiation state machine. Default value " + DEFAULT_BATCH_SIZE, type = "int")
    private static final String NEGOTIATION_CONSUMER_STATE_MACHINE_BATCH_SIZE = "edc.negotiation.consumer.state-machine.batch-size";

    @Setting(value = "the batch size in the provider negotiation state machine. Default value " + DEFAULT_BATCH_SIZE, type = "int")
    private static final String NEGOTIATION_PROVIDER_STATE_MACHINE_BATCH_SIZE = "edc.negotiation.provider.state-machine.batch-size";

    @Setting(value = "how many times a specific operation must be tried before terminating the consumer negotiation with error", type = "int", defaultValue = DEFAULT_SEND_RETRY_LIMIT + "")
    private static final String NEGOTIATION_CONSUMER_SEND_RETRY_LIMIT = "edc.negotiation.consumer.send.retry.limit";

    @Setting(value = "how many times a specific operation must be tried before terminating the provider negotiation with error", type = "int", defaultValue = DEFAULT_SEND_RETRY_LIMIT + "")
    private static final String NEGOTIATION_PROVIDER_SEND_RETRY_LIMIT = "edc.negotiation.provider.send.retry.limit";

    @Setting(value = "The base delay for the consumer negotiation retry mechanism in millisecond", type = "long", defaultValue = DEFAULT_SEND_RETRY_BASE_DELAY + "")
    private static final String NEGOTIATION_CONSUMER_SEND_RETRY_BASE_DELAY_MS = "edc.negotiation.consumer.send.retry.base-delay.ms";

    @Setting(value = "The base delay for the provider negotiation retry mechanism in millisecond", type = "long", defaultValue = DEFAULT_SEND_RETRY_BASE_DELAY + "")
    private static final String NEGOTIATION_PROVIDER_SEND_RETRY_BASE_DELAY_MS = "edc.negotiation.provider.send.retry.base-delay.ms";

    private ConsumerContractNegotiationManagerImpl consumerNegotiationManager;

    private ProviderContractNegotiationManagerImpl providerNegotiationManager;

    @Inject
    private AssetIndex assetIndex;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Inject
    private ContractNegotiationStore store;

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

    @Inject
    private EventRouter eventRouter;

    @Inject
    private TypeManager typeManager;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Inject
    private ProtocolWebhook protocolWebhook;

    @Inject
    private ContractNegotiationObservable observable;

    @Inject
    private ContractNegotiationPendingGuard pendingGuard;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        typeManager.registerTypes(ContractNegotiation.class);
        policyEngine.registerScope(CATALOG_SCOPE, CatalogPolicyContext.class);
        policyEngine.registerScope(NEGOTIATION_SCOPE, ContractNegotiationPolicyContext.class);
        policyEngine.registerScope(TRANSFER_SCOPE, TransferProcessPolicyContext.class);
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
        var participantId = context.getParticipantId();

        var policyEquality = new PolicyEquality(typeManager);
        var validationService = new ContractValidationServiceImpl(assetIndex, policyEngine, policyEquality);
        context.registerService(ContractValidationService.class, validationService);

        // bind/register rule to evaluate contract expiry
        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, TRANSFER_SCOPE);
        ruleBindingRegistry.bind(CONTRACT_EXPIRY_EVALUATION_KEY, TRANSFER_SCOPE);
        var function = new ContractExpiryFunction();
        policyEngine.registerFunction(TransferProcessPolicyContext.class, Permission.class, CONTRACT_EXPIRY_EVALUATION_KEY, function);

        var iterationWaitMillis = context.getSetting(NEGOTIATION_STATE_MACHINE_ITERATION_WAIT_MILLIS, DEFAULT_ITERATION_WAIT);
        var waitStrategy = context.hasService(NegotiationWaitStrategy.class) ? context.getService(NegotiationWaitStrategy.class) : new ExponentialWaitStrategy(iterationWaitMillis);

        observable.registerListener(new ContractNegotiationEventListener(eventRouter, clock));

        consumerNegotiationManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .participantId(participantId)
                .waitStrategy(waitStrategy)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .observable(observable)
                .clock(clock)
                .telemetry(telemetry)
                .executorInstrumentation(executorInstrumentation)
                .store(store)
                .policyStore(policyStore)
                .batchSize(context.getSetting(NEGOTIATION_CONSUMER_STATE_MACHINE_BATCH_SIZE, DEFAULT_BATCH_SIZE))
                .entityRetryProcessConfiguration(consumerEntityRetryProcessConfiguration(context))
                .protocolWebhook(protocolWebhook)
                .pendingGuard(pendingGuard)
                .build();

        providerNegotiationManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .participantId(participantId)
                .waitStrategy(waitStrategy)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .observable(observable)
                .clock(clock)
                .telemetry(telemetry)
                .executorInstrumentation(executorInstrumentation)
                .store(store)
                .policyStore(policyStore)
                .batchSize(context.getSetting(NEGOTIATION_PROVIDER_STATE_MACHINE_BATCH_SIZE, DEFAULT_BATCH_SIZE))
                .entityRetryProcessConfiguration(providerEntityRetryProcessConfiguration(context))
                .protocolWebhook(protocolWebhook)
                .pendingGuard(pendingGuard)
                .build();

        context.registerService(ConsumerContractNegotiationManager.class, consumerNegotiationManager);
        context.registerService(ProviderContractNegotiationManager.class, providerNegotiationManager);
    }

    private EntityRetryProcessConfiguration providerEntityRetryProcessConfiguration(ServiceExtensionContext context) {
        var retryLimit = context.getSetting(NEGOTIATION_PROVIDER_SEND_RETRY_LIMIT, DEFAULT_SEND_RETRY_LIMIT);
        var retryBaseDelay = context.getSetting(NEGOTIATION_PROVIDER_SEND_RETRY_BASE_DELAY_MS, DEFAULT_SEND_RETRY_BASE_DELAY);
        return new EntityRetryProcessConfiguration(retryLimit, () -> new ExponentialWaitStrategy(retryBaseDelay));
    }

    @NotNull
    private EntityRetryProcessConfiguration consumerEntityRetryProcessConfiguration(ServiceExtensionContext context) {
        var retryLimit = context.getSetting(NEGOTIATION_CONSUMER_SEND_RETRY_LIMIT, DEFAULT_SEND_RETRY_LIMIT);
        var retryBaseDelay = context.getSetting(NEGOTIATION_CONSUMER_SEND_RETRY_BASE_DELAY_MS, DEFAULT_SEND_RETRY_BASE_DELAY);
        return new EntityRetryProcessConfiguration(retryLimit, () -> new ExponentialWaitStrategy(retryBaseDelay));
    }

}
