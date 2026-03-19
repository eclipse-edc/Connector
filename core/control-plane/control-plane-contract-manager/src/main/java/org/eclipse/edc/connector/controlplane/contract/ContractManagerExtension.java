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
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.NegotiationProcessors;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.NegotiationWaitStrategy;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ProviderContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
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

    @SettingContext("edc.negotiation")
    @Configuration
    private StateMachineConfiguration stateMachineConfiguration;

    @Inject
    private ContractNegotiationStore store;
    @Inject
    private Monitor monitor;
    @Inject
    private Telemetry telemetry;
    @Inject
    private Clock clock;
    @Inject
    private ContractNegotiationPendingGuard pendingGuard;
    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    @Inject
    private NegotiationProcessors negotiationProcessors;

    private ConsumerContractNegotiationManagerImpl consumerNegotiationManager;
    private ProviderContractNegotiationManagerImpl providerNegotiationManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        checkConfigurationGroup(context, "edc.negotiation.provider");
        checkConfigurationGroup(context, "edc.negotiation.consumer");

        var waitStrategy = context.hasService(NegotiationWaitStrategy.class)
                ? context.getService(NegotiationWaitStrategy.class)
                : stateMachineConfiguration.iterationWaitExponentialWaitStrategy();

        consumerNegotiationManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .negotiationProcessors(negotiationProcessors)
                .waitStrategy(waitStrategy)
                .monitor(monitor)
                .clock(clock)
                .telemetry(telemetry)
                .executorInstrumentation(executorInstrumentation)
                .store(store)
                .batchSize(stateMachineConfiguration.batchSize())
                .entityRetryProcessConfiguration(stateMachineConfiguration.entityRetryProcessConfiguration())
                .pendingGuard(pendingGuard)
                .build();

        providerNegotiationManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .negotiationProcessors(negotiationProcessors)
                .waitStrategy(waitStrategy)
                .monitor(monitor)
                .clock(clock)
                .telemetry(telemetry)
                .executorInstrumentation(executorInstrumentation)
                .store(store)
                .batchSize(stateMachineConfiguration.batchSize())
                .entityRetryProcessConfiguration(stateMachineConfiguration.entityRetryProcessConfiguration())
                .pendingGuard(pendingGuard)
                .build();

        context.registerService(ConsumerContractNegotiationManager.class, consumerNegotiationManager);
        context.registerService(ProviderContractNegotiationManager.class, providerNegotiationManager);
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

    @Deprecated(since = "0.17.0")
    private void checkConfigurationGroup(ServiceExtensionContext context, String configurationGroup) {
        if (!context.getConfig(configurationGroup).getEntries().isEmpty()) {
            var message = "The configuration group '" + configurationGroup + "' needs to be migrated into 'edc.negotiation'";
            monitor.severe(message);
            throw new EdcException(message);
        }
    }

}
