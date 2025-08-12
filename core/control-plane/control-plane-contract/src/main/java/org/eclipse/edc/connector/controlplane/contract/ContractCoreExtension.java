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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - add contract negotiation functionality
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       Microsoft Corporation - improvements
 *
 */

package org.eclipse.edc.connector.controlplane.contract;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.listener.ContractNegotiationEventListener;
import org.eclipse.edc.connector.controlplane.contract.policy.PolicyEquality;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.controlplane.contract.validation.ContractValidationServiceImpl;
import org.eclipse.edc.connector.controlplane.policy.contract.ContractExpiryCheckFunction;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.TypeManager;

import java.time.Clock;

import static org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext.CATALOG_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext.NEGOTIATION_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext.TRANSFER_SCOPE;
import static org.eclipse.edc.connector.controlplane.policy.contract.ContractExpiryCheckFunction.CONTRACT_EXPIRY_EVALUATION_KEY;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;

@Provides({
        ContractValidationService.class
})
@Extension(value = ContractCoreExtension.NAME)
public class ContractCoreExtension implements ServiceExtension {

    public static final String NAME = "Contract Core";

    @Inject
    private AssetIndex assetIndex;

    @Inject
    private PolicyEngine policyEngine;

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
    private ContractNegotiationObservable observable;

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

    private void registerServices(ServiceExtensionContext context) {
        var policyEquality = new PolicyEquality(typeManager);
        var validationService = new ContractValidationServiceImpl(assetIndex, policyEngine, policyEquality);
        context.registerService(ContractValidationService.class, validationService);

        // bind/register rule to evaluate contract expiry
        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, TRANSFER_SCOPE);
        ruleBindingRegistry.bind(CONTRACT_EXPIRY_EVALUATION_KEY, TRANSFER_SCOPE);

        policyEngine.registerFunction(TransferProcessPolicyContext.class, Permission.class, CONTRACT_EXPIRY_EVALUATION_KEY,
                new ContractExpiryCheckFunction<>());

        observable.registerListener(new ContractNegotiationEventListener(eventRouter));
    }
}
