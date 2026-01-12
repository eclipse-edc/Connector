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

package org.eclipse.edc.connector.controlplane.transfer;

import org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.process.TransferProcessManagerImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.retry.TransferWaitStrategy;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.statemachine.StateMachineConfiguration;

import java.time.Clock;

/**
 * Provides transfer manager service to the system.
 */
@Provides(TransferProcessManager.class)
@Extension(value = TransferManagerExtension.NAME)
public class TransferManagerExtension implements ServiceExtension {

    public static final String NAME = "Transfer Manager";

    @SettingContext("edc.transfer")
    @Configuration
    private StateMachineConfiguration stateMachineConfiguration;

    @Inject
    private TransferProcessStore transferProcessStore;

    @Inject
    private DataFlowController dataFlowController;

    @Inject
    private TransferProcessObservable observable;

    @Inject
    private PolicyArchive policyArchive;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Inject
    private DataAddressResolver addressResolver;

    @Inject
    private Vault vault;

    @Inject
    private Clock clock;

    @Inject
    private Telemetry telemetry;

    @Inject
    private DataspaceProfileContextRegistry dataspaceProfileContextRegistry;

    @Inject
    private TransferProcessPendingGuard pendingGuard;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    private TransferProcessManagerImpl processManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var waitStrategy = context.hasService(TransferWaitStrategy.class) ? context.getService(TransferWaitStrategy.class) : stateMachineConfiguration.iterationWaitExponentialWaitStrategy();

        var entityRetryProcessConfiguration = stateMachineConfiguration.entityRetryProcessConfiguration();

        processManager = TransferProcessManagerImpl.Builder.newInstance()
                .waitStrategy(waitStrategy)
                .dataFlowController(dataFlowController)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .telemetry(telemetry)
                .executorInstrumentation(executorInstrumentation)
                .vault(vault)
                .clock(clock)
                .observable(observable)
                .store(transferProcessStore)
                .policyArchive(policyArchive)
                .batchSize(stateMachineConfiguration.batchSize())
                .addressResolver(addressResolver)
                .entityRetryProcessConfiguration(entityRetryProcessConfiguration)
                .dataspaceProfileContextRegistry(dataspaceProfileContextRegistry)
                .pendingGuard(pendingGuard)
                .build();

        context.registerService(TransferProcessManager.class, processManager);

    }

    @Override
    public void start() {
        processManager.start();
    }

    @Override
    public void shutdown() {
        if (processManager != null) {
            processManager.stop();
        }
    }

}
