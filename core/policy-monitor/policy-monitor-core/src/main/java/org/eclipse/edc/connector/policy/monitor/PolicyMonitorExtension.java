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

import org.eclipse.edc.connector.controlplane.policy.contract.ContractExpiryCheckFunction;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.policy.monitor.manager.PolicyMonitorManagerImpl;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorManager;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.connector.policy.monitor.subscriber.StartMonitoring;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
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

import static org.eclipse.edc.connector.controlplane.policy.contract.ContractExpiryCheckFunction.CONTRACT_EXPIRY_EVALUATION_KEY;
import static org.eclipse.edc.connector.policy.monitor.PolicyMonitorExtension.NAME;
import static org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext.POLICY_MONITOR_SCOPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_BATCH_SIZE;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_ITERATION_WAIT;

@Extension(value = NAME)
@Provides({ PolicyMonitorManager.class })
public class PolicyMonitorExtension implements ServiceExtension {

    public static final String NAME = "Policy Monitor";

    @Setting(description = "the iteration wait time in milliseconds in the policy monitor state machine. Default value " + DEFAULT_ITERATION_WAIT,
            key = "edc.policy.monitor.state-machine.iteration-wait-millis", defaultValue = DEFAULT_ITERATION_WAIT + "")
    private long iterationWaitMillis;

    @Setting(description = "the batch size in the policy monitor state machine. Default value " + DEFAULT_BATCH_SIZE, key = "edc.policy.monitor.state-machine.batch-size", defaultValue = DEFAULT_BATCH_SIZE + "")
    private int batchSize;

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

    @Inject
    private PolicyMonitorStore policyMonitorStore;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    private PolicyMonitorManager manager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var waitStrategy = new ExponentialWaitStrategy(iterationWaitMillis);

        policyEngine.registerScope(POLICY_MONITOR_SCOPE, PolicyMonitorContext.class);
        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, POLICY_MONITOR_SCOPE);
        ruleBindingRegistry.bind(CONTRACT_EXPIRY_EVALUATION_KEY, POLICY_MONITOR_SCOPE);
        policyEngine.registerFunction(PolicyMonitorContext.class, Permission.class, CONTRACT_EXPIRY_EVALUATION_KEY, new ContractExpiryCheckFunction<>());

        manager = PolicyMonitorManagerImpl.Builder.newInstance()
                .clock(clock)
                .batchSize(batchSize)
                .waitStrategy(waitStrategy)
                .executorInstrumentation(executorInstrumentation)
                .monitor(context.getMonitor())
                .telemetry(telemetry)
                .contractAgreementService(contractAgreementService)
                .policyEngine(policyEngine)
                .transferProcessService(transferProcessService)
                .store(policyMonitorStore)
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
