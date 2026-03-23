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
import org.eclipse.edc.connector.policy.monitor.manager.PolicyMonitor;
import org.eclipse.edc.connector.policy.monitor.manager.PolicyMonitorManagerImpl;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorManager;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.connector.policy.monitor.subscriber.StartMonitoring;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

import static org.eclipse.edc.connector.controlplane.policy.contract.ContractExpiryCheckFunction.CONTRACT_EXPIRY_EVALUATION_KEY;
import static org.eclipse.edc.connector.policy.monitor.PolicyMonitorExtension.NAME;
import static org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext.POLICY_MONITOR_SCOPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;

@Extension(value = NAME)
@Provides({ PolicyMonitorManager.class })
public class PolicyMonitorExtension implements ServiceExtension {

    public static final String NAME = "Policy Monitor";

    @SettingContext("edc.policy.monitor")
    @Configuration
    private PolicyMonitorConfiguration configuration;

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
    @Inject
    private TransactionContext transactionContext;

    private PolicyMonitorManager manager;

    @Override
    public void initialize(ServiceExtensionContext context) {

        failWhenSettingsAreNotUpdated(context);

        policyEngine.registerScope(POLICY_MONITOR_SCOPE, PolicyMonitorContext.class);
        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, POLICY_MONITOR_SCOPE);
        ruleBindingRegistry.bind(CONTRACT_EXPIRY_EVALUATION_KEY, POLICY_MONITOR_SCOPE);
        policyEngine.registerFunction(PolicyMonitorContext.class, Permission.class, CONTRACT_EXPIRY_EVALUATION_KEY, new ContractExpiryCheckFunction<>());

        var policyMonitor = new PolicyMonitor(policyMonitorStore, telemetry, transferProcessService,
                contractAgreementService, policyEngine, context.getMonitor(), clock, transactionContext);

        manager = new PolicyMonitorManagerImpl(policyMonitor, configuration, executorInstrumentation, context.getMonitor(), policyMonitorStore, clock);

        context.registerService(PolicyMonitorManager.class, manager);

        eventRouter.registerSync(TransferProcessStarted.class, new StartMonitoring(policyMonitor));
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

    @Deprecated(since = "0.17.0")
    private void failWhenSettingsAreNotUpdated(ServiceExtensionContext context) {
        if (!context.getConfig("edc.policy.monitor.state-machine").getEntries().isEmpty()) {
            var message = """
                    Policy Monitor model has been changed from a state machine to a watchdog, please
                    review the configuration accordingly: 'edc.policy.manager.batch-size' to set up the batch size,
                    'edc.policy.manager.period' to set up the period in ISO-8061 Duration format. The
                    'edc.policy.manager.state-machine' settings must be deleted.
                    """;
            context.getMonitor().severe(message);
            throw new RuntimeException(message);
        }
    }

}
