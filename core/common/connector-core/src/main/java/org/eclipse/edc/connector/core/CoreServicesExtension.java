/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Cofinity-X - make participant id extraction dependent on dataspace profile context
 *
 */

package org.eclipse.edc.connector.core;

import org.eclipse.edc.api.auth.spi.ControlClientAuthenticationProvider;
import org.eclipse.edc.connector.core.agent.ParticipantAgentServiceImpl;
import org.eclipse.edc.connector.core.validator.DataAddressValidatorRegistryImpl;
import org.eclipse.edc.http.client.ControlApiHttpClientImpl;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.participant.spi.ParticipantAgentService;
import org.eclipse.edc.policy.engine.PolicyEngineImpl;
import org.eclipse.edc.policy.engine.RuleBindingRegistryImpl;
import org.eclipse.edc.policy.engine.ScopeFilter;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.engine.validation.RuleValidator;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;

@Extension(value = CoreServicesExtension.NAME)
public class CoreServicesExtension implements ServiceExtension {

    public static final String NAME = "Core Services";

    @Inject
    private TypeManager typeManager;

    @Inject
    private EdcHttpClient edcHttpClient;

    @Inject
    private ControlClientAuthenticationProvider controlClientAuthenticationProvider;

    private RuleBindingRegistry ruleBindingRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        ruleBindingRegistry = new RuleBindingRegistryImpl();
    }

    @Override
    public void prepare() {
        PolicyRegistrationTypes.TYPES.forEach(typeManager::registerTypes);
    }

    @Provider
    public ParticipantAgentService participantAgentService() {
        return new ParticipantAgentServiceImpl();
    }

    @Provider
    public RuleBindingRegistry ruleBindingRegistry() {
        return ruleBindingRegistry;
    }

    @Provider
    public PolicyEngine policyEngine() {
        var scopeFilter = new ScopeFilter(ruleBindingRegistry);
        var ruleValidator = new RuleValidator(ruleBindingRegistry);
        return new PolicyEngineImpl(scopeFilter, ruleValidator);
    }

    @Provider
    public DataAddressValidatorRegistry dataAddressValidatorRegistry(ServiceExtensionContext context) {
        return new DataAddressValidatorRegistryImpl(context.getMonitor());
    }

    @Provider
    public ControlApiHttpClient controlApiHttpClient() {
        return new ControlApiHttpClientImpl(edcHttpClient, controlClientAuthenticationProvider);
    }

}

