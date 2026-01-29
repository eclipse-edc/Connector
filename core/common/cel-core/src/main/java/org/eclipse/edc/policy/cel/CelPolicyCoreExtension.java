/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.policy.cel;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext;
import org.eclipse.edc.policy.cel.engine.CelExpressionEngine;
import org.eclipse.edc.policy.cel.engine.CelExpressionEngineImpl;
import org.eclipse.edc.policy.cel.function.CelExpressionFunction;
import org.eclipse.edc.policy.cel.function.context.AgreementContextMapper;
import org.eclipse.edc.policy.cel.function.context.ParticipantAgentContextMapper;
import org.eclipse.edc.policy.cel.function.context.PolicyMonitorContextMapper;
import org.eclipse.edc.policy.cel.function.context.TransferProcessContextMapper;
import org.eclipse.edc.policy.cel.service.CelPolicyExpressionService;
import org.eclipse.edc.policy.cel.service.CelPolicyExpressionServiceImpl;
import org.eclipse.edc.policy.cel.store.CelExpressionStore;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;

import static org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext.CATALOG_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext.NEGOTIATION_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext.TRANSFER_SCOPE;
import static org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext.POLICY_MONITOR_SCOPE;
import static org.eclipse.edc.policy.cel.CelPolicyCoreExtension.NAME;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;

@Extension(NAME)
public class CelPolicyCoreExtension implements ServiceExtension {

    public static final String NAME = "Common Expression Language Policy Core Extension";

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private CelExpressionStore celExpressionStore;

    @Inject
    private TransactionContext transactionContext;

    private CelExpressionEngine celExpressionEngine;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }


    @Override
    public void initialize(ServiceExtensionContext context) {

        ruleBindingRegistry.dynamicBind(policyExpressionEngine()::evaluationScopes);

        List.of(Permission.class, Duty.class, Prohibition.class).forEach(c -> {
            bindFunction(new CelExpressionFunction<>(policyExpressionEngine(), new TransferProcessContextMapper(new AgreementContextMapper(), new ParticipantAgentContextMapper<>())), c, TransferProcessPolicyContext.class);
            bindFunction(new CelExpressionFunction<>(policyExpressionEngine(), new ParticipantAgentContextMapper<>()), c, ContractNegotiationPolicyContext.class);
            bindFunction(new CelExpressionFunction<>(policyExpressionEngine(), new ParticipantAgentContextMapper<>()), c, CatalogPolicyContext.class);
            bindFunction(new CelExpressionFunction<>(policyExpressionEngine(), new PolicyMonitorContextMapper(new AgreementContextMapper())), c, PolicyMonitorContext.class);
        });

        List.of(CATALOG_SCOPE, TRANSFER_SCOPE, NEGOTIATION_SCOPE, POLICY_MONITOR_SCOPE).forEach(scope -> {
            ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        });

    }

    @Provider
    public CelExpressionEngine policyExpressionEngine() {
        if (celExpressionEngine == null) {
            celExpressionEngine = new CelExpressionEngineImpl(transactionContext, celExpressionStore, monitor);
        }
        return celExpressionEngine;
    }

    @Provider
    public CelPolicyExpressionService policyExpressionService() {
        return new CelPolicyExpressionServiceImpl(celExpressionStore, transactionContext, policyExpressionEngine());
    }

    private <C extends PolicyContext, R extends Rule> void bindFunction(DynamicAtomicConstraintRuleFunction<R, C> function, Class<R> ruleClass, Class<C> contextClass) {
        policyEngine.registerFunction(contextClass, ruleClass, function);
    }

}
