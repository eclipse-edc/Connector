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
import org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.statemachine.StateMachineConfiguration;

import java.time.Clock;

@Provides({ConsumerContractNegotiationManager.class, ProviderContractNegotiationManager.class})
@Extension(value = ContractManagerExtension.NAME)
public class ContractManagerExtension implements ServiceExtension {

    public static final String NAME = "Contract Manager";

    @SettingContext("edc.negotiation.consumer")
    @Configuration
    private StateMachineConfiguration consumerStateMachineConfiguration;

    @SettingContext("edc.negotiation.provider")
    @Configuration
    private StateMachineConfiguration providerStateMachineConfiguration;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Inject
    private ContractNegotiationStore store;

    @Inject
    private PolicyDefinitionStore policyStore;

    @Inject
    private Monitor monitor;

    @Inject
    private Telemetry telemetry;

    @Inject
    private Clock clock;

    @Inject
    private DataspaceProfileContextRegistry dataspaceProfileContextRegistry;

    @Inject
    private ContractNegotiationObservable observable;

    @Inject
    private ContractNegotiationPendingGuard pendingGuard;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    @Inject
    private ParticipantIdentityResolver identityResolver;

    private ConsumerContractNegotiationManagerImpl consumerNegotiationManager;
    private ProviderContractNegotiationManagerImpl providerNegotiationManager;

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
        consumerNegotiationManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .waitStrategy(getWaitStrategy(context, consumerStateMachineConfiguration.iterationWaitExponentialWaitStrategy()))
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
                .identityResolver(identityResolver)
                .pendingGuard(pendingGuard)
                .build();

        providerNegotiationManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .waitStrategy(getWaitStrategy(context, providerStateMachineConfiguration.iterationWaitExponentialWaitStrategy()))
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
                .identityResolver(identityResolver)
                .pendingGuard(pendingGuard)
                .build();

        context.registerService(ConsumerContractNegotiationManager.class, consumerNegotiationManager);
        context.registerService(ProviderContractNegotiationManager.class, providerNegotiationManager);
    }

    private WaitStrategy getWaitStrategy(ServiceExtensionContext context, ExponentialWaitStrategy fallback) {
        return context.hasService(NegotiationWaitStrategy.class) ? context.getService(NegotiationWaitStrategy.class) : fallback;
    }

}
