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
 *
 */

package org.eclipse.edc.connector.core;

import org.eclipse.edc.api.auth.spi.ControlClientAuthenticationProvider;
import org.eclipse.edc.connector.core.agent.ParticipantAgentServiceImpl;
import org.eclipse.edc.connector.core.command.CommandHandlerRegistryImpl;
import org.eclipse.edc.connector.core.event.EventExecutorServiceContainer;
import org.eclipse.edc.connector.core.event.EventRouterImpl;
import org.eclipse.edc.connector.core.message.RemoteMessageDispatcherRegistryImpl;
import org.eclipse.edc.connector.core.validator.DataAddressValidatorRegistryImpl;
import org.eclipse.edc.connector.core.validator.JsonObjectValidatorRegistryImpl;
import org.eclipse.edc.http.client.ControlApiHttpClientImpl;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.participant.spi.ParticipantAgentService;
import org.eclipse.edc.policy.engine.PolicyEngineImpl;
import org.eclipse.edc.policy.engine.RuleBindingRegistryImpl;
import org.eclipse.edc.policy.engine.ScopeFilter;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.engine.validation.RuleValidator;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static org.eclipse.edc.participant.spi.ParticipantAgentService.DEFAULT_IDENTITY_CLAIM_KEY;

@Extension(value = CoreServicesExtension.NAME)
public class CoreServicesExtension implements ServiceExtension {

    public static final String NAME = "Core Services";
    @Setting(description = "The name of the claim key used to determine the participant identity", defaultValue = DEFAULT_IDENTITY_CLAIM_KEY)
    public static final String EDC_AGENT_IDENTITY_KEY = "edc.agent.identity.key";
    private static final String DEFAULT_EDC_HOSTNAME = "localhost";
    public static final String EDC_HOSTNAME = "edc.hostname";
    @Setting(description = "Connector hostname, which e.g. is used in referer urls", defaultValue = DEFAULT_EDC_HOSTNAME, key = EDC_HOSTNAME, warnOnMissingConfig = true)
    public static String hostname;
    @Inject
    private EventExecutorServiceContainer eventExecutorServiceContainer;

    @Inject(required = false)
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
    public void shutdown() {
        ServiceExtension.super.shutdown();
    }

    @Override
    public void prepare() {
        PolicyRegistrationTypes.TYPES.forEach(typeManager()::registerTypes);
    }

    @Provider
    public TypeManager typeManager() {
        if (typeManager == null) {
            typeManager = new JacksonTypeManager();
        }
        return typeManager;
    }

    @Provider
    public Hostname hostname(ServiceExtensionContext context) {
        return () -> hostname;
    }

    @Provider
    public RemoteMessageDispatcherRegistry remoteMessageDispatcherRegistry() {
        return new RemoteMessageDispatcherRegistryImpl();
    }

    @Provider
    public CommandHandlerRegistry commandHandlerRegistry() {
        return new CommandHandlerRegistryImpl();
    }

    @Provider
    public ParticipantAgentService participantAgentService(ServiceExtensionContext context) {
        var identityKey = context.getSetting(EDC_AGENT_IDENTITY_KEY, DEFAULT_IDENTITY_CLAIM_KEY);
        return new ParticipantAgentServiceImpl(identityKey);
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
    public EventRouter eventRouter(ServiceExtensionContext context) {
        return new EventRouterImpl(context.getMonitor(), eventExecutorServiceContainer.getExecutorService());
    }

    @Provider
    public TypeTransformerRegistry typeTransformerRegistry() {
        return new TypeTransformerRegistryImpl();
    }

    @Provider
    public JsonObjectValidatorRegistry jsonObjectValidator() {
        return new JsonObjectValidatorRegistryImpl();
    }

    @Provider
    public DataAddressValidatorRegistry dataAddressValidatorRegistry(ServiceExtensionContext context) {
        return new DataAddressValidatorRegistryImpl(context.getMonitor());
    }

    @Provider
    public CriterionOperatorRegistry criterionOperatorRegistry() {
        return CriterionOperatorRegistryImpl.ofDefaults();
    }

    @Provider
    public ControlApiHttpClient controlApiHttpClient() {
        return new ControlApiHttpClientImpl(edcHttpClient, controlClientAuthenticationProvider);
    }

}

