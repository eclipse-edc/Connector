/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.policy.monitor;

import org.eclipse.edc.connector.policy.monitor.manager.PolicyMonitorManagerImpl;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorManager;
import org.eclipse.edc.connector.policy.monitor.store.InMemoryPolicyMonitorStore;
import org.eclipse.edc.connector.policy.monitor.subscriber.StartMonitoring;
import org.eclipse.edc.connector.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;

import java.time.Clock;

import static org.eclipse.edc.connector.core.entity.AbstractStateEntityManager.DEFAULT_BATCH_SIZE;
import static org.eclipse.edc.connector.core.entity.AbstractStateEntityManager.DEFAULT_ITERATION_WAIT;
import static org.eclipse.edc.connector.policy.monitor.PolicyMonitorExtension.NAME;

@Extension(value = NAME)
@Provides({ PolicyMonitorManager.class })
public class PolicyMonitorExtension implements ServiceExtension {

    public static final String NAME = "Policy Monitor";

    @Setting(value = "the iteration wait time in milliseconds in the policy monitor state machine. Default value " + DEFAULT_ITERATION_WAIT, type = "long")
    private static final String POLICY_MONITOR_ITERATION_WAIT_MILLIS = "edc.policy.monitor.state-machine.iteration-wait-millis";

    @Setting(value = "the batch size in the policy monitor state machine. Default value " + DEFAULT_BATCH_SIZE, type = "int")
    private static final String POLICY_MONITOR_BATCH_SIZE = "edc.policy.monitor.state-machine.batch-size";

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    @Inject
    private Telemetry telemetry;

    @Inject
    private Clock clock;

    @Inject
    private EventRouter eventRouter;

    @Inject
    private ContractAgreementService contractAgreementService;

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private TransferProcessService transferProcessService;

    private PolicyMonitorManager manager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var iterationWaitMillis = context.getSetting(POLICY_MONITOR_ITERATION_WAIT_MILLIS, DEFAULT_ITERATION_WAIT);
        var waitStrategy = new ExponentialWaitStrategy(iterationWaitMillis);

        manager = PolicyMonitorManagerImpl.Builder.newInstance()
                .clock(clock)
                .batchSize(context.getSetting(POLICY_MONITOR_BATCH_SIZE, DEFAULT_BATCH_SIZE))
                .waitStrategy(waitStrategy)
                .executorInstrumentation(executorInstrumentation)
                .monitor(context.getMonitor())
                .telemetry(telemetry)
                .contractAgreementService(contractAgreementService)
                .policyEngine(policyEngine)
                .transferProcessService(transferProcessService)
                .store(new InMemoryPolicyMonitorStore())
                .build();

        context.registerService(PolicyMonitorManager.class, manager);

        eventRouter.registerSync(TransferProcessStarted.class, new StartMonitoring(manager));
    }

    @Override
    public void start() {
        manager.start();
    }

    @Override
    public void shutdown() {
        if (manager != null) {
            manager.stop();
        }
    }

}
