/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract;

import org.eclipse.edc.connector.controlplane.contract.negotiation.ConsumerContractNegotiationManagerImpl;
import org.eclipse.edc.connector.controlplane.contract.negotiation.ProviderContractNegotiationManagerImpl;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.NegotiationWaitStrategy;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ProviderContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.statemachine.StateMachineConfiguration;

import java.time.Clock;

@Provides({ConsumerContractNegotiationManager.class, ProviderContractNegotiationManager.class
})
@Extension(value = ContractManagerExtension.NAME)
public class ContractManagerExtension implements ServiceExtension {

    public static final String NAME = "Contract Manager";
    private static final String DEPRECATED_ITERATION_WAIT_MILLIS_KEY = "edc.negotiation.state-machine.iteration-wait-millis";

    @Deprecated(since = "0.14.0")
    @Setting(
            description = "the iteration wait time in milliseconds in the negotiation state machine.",
            key = DEPRECATED_ITERATION_WAIT_MILLIS_KEY,
            defaultValue = StateMachineConfiguration.DEFAULT_ITERATION_WAIT + "")
    private long stateMachineIterationWaitMillis;

    @SettingContext("edc.negotiation.consumer")
    @Configuration
    private StateMachineConfiguration consumerStateMachineConfiguration;

    @SettingContext("edc.negotiation.provider")
    @Configuration
    private StateMachineConfiguration providerStateMachineConfiguration;

    private ConsumerContractNegotiationManagerImpl consumerNegotiationManager;

    private ProviderContractNegotiationManagerImpl providerNegotiationManager;

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
    private DataspaceProfileContextRegistry dataspaceProfileContextRegistry;

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

        WaitStrategy consumerWaitStrategy;
        WaitStrategy providerWaitStrategy;
        if (context.getConfig().hasKey(DEPRECATED_ITERATION_WAIT_MILLIS_KEY)) {
            monitor.warning(("The setting '%s' has been deprecated, please use 'edc.negotiation.consumer.state-machine.iteration-wait-millis' " +
                    "and 'edc.negotiation.provider.state-machine.iteration-wait-millis' instead.")
                    .formatted(DEPRECATED_ITERATION_WAIT_MILLIS_KEY));
            consumerWaitStrategy = getWaitStrategy(context, new ExponentialWaitStrategy(stateMachineIterationWaitMillis));
            providerWaitStrategy = getWaitStrategy(context, new ExponentialWaitStrategy(stateMachineIterationWaitMillis));
        } else {
            consumerWaitStrategy = getWaitStrategy(context, consumerStateMachineConfiguration.iterationWaitExponentialWaitStrategy());
            providerWaitStrategy = getWaitStrategy(context, providerStateMachineConfiguration.iterationWaitExponentialWaitStrategy());
        }

        consumerNegotiationManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .participantId(participantId)
                .waitStrategy(consumerWaitStrategy)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .observable(observable)
                .clock(clock)
                .telemetry(telemetry)
                .executorInstrumentation(executorInstrumentation)
                .store(store)
                .policyStore(policyStore)
                .batchSize(consumerStateMachineConfiguration.batchSize())
                .entityRetryProcessConfiguration(consumerStateMachineConfiguration.entityRetryProcessConfiguration())
                .dataspaceProfileContextRegistry(dataspaceProfileContextRegistry)
                .pendingGuard(pendingGuard)
                .build();

        providerNegotiationManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .participantId(participantId)
                .waitStrategy(providerWaitStrategy)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .observable(observable)
                .clock(clock)
                .telemetry(telemetry)
                .executorInstrumentation(executorInstrumentation)
                .store(store)
                .policyStore(policyStore)
                .batchSize(providerStateMachineConfiguration.batchSize())
                .entityRetryProcessConfiguration(providerStateMachineConfiguration.entityRetryProcessConfiguration())
                .dataspaceProfileContextRegistry(dataspaceProfileContextRegistry)
                .pendingGuard(pendingGuard)
                .build();

        context.registerService(ConsumerContractNegotiationManager.class, consumerNegotiationManager);
        context.registerService(ProviderContractNegotiationManager.class, providerNegotiationManager);
    }

    private WaitStrategy getWaitStrategy(ServiceExtensionContext context, ExponentialWaitStrategy fallback) {
        return context.hasService(NegotiationWaitStrategy.class) ? context.getService(NegotiationWaitStrategy.class) : fallback;
    }

}
