/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.controlplane.api.management.policy;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.api.management.policy.transform.JsonObjectFromPolicyDefinitionTransformer;
import org.eclipse.edc.connector.controlplane.api.management.policy.transform.JsonObjectFromPolicyEvaluationPlanTransformer;
import org.eclipse.edc.connector.controlplane.api.management.policy.transform.JsonObjectFromPolicyValidationResultTransformer;
import org.eclipse.edc.connector.controlplane.api.management.policy.transform.JsonObjectToPolicyDefinitionTransformer;
import org.eclipse.edc.connector.controlplane.api.management.policy.transform.JsonObjectToPolicyEvaluationPlanRequestTransformer;
import org.eclipse.edc.connector.controlplane.api.management.policy.v3.PolicyDefinitionApiV3Controller;
import org.eclipse.edc.connector.controlplane.api.management.policy.validation.PolicyDefinitionValidator;
import org.eclipse.edc.connector.controlplane.api.management.policy.validation.PolicyEvaluationPlanRequestValidator;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyEvaluationPlanRequest.EDC_POLICY_EVALUATION_PLAN_REQUEST_TYPE;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;


@Extension(value = PolicyDefinitionApiExtension.NAME)
public class PolicyDefinitionApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Policy Definition";

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private WebService webService;

    @Inject
    private PolicyDefinitionService service;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jsonBuilderFactory = Json.createBuilderFactory(Map.of());
        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");
        managementApiTransformerRegistry.register(new JsonObjectToPolicyEvaluationPlanRequestTransformer());
        managementApiTransformerRegistry.register(new JsonObjectToPolicyDefinitionTransformer());
        managementApiTransformerRegistry.register(new JsonObjectFromPolicyDefinitionTransformer(jsonBuilderFactory, typeManager, JSON_LD));
        managementApiTransformerRegistry.register(new JsonObjectFromPolicyValidationResultTransformer(jsonBuilderFactory));
        managementApiTransformerRegistry.register(new JsonObjectFromPolicyEvaluationPlanTransformer(jsonBuilderFactory));

        validatorRegistry.register(EDC_POLICY_DEFINITION_TYPE, PolicyDefinitionValidator.instance());
        validatorRegistry.register(EDC_POLICY_EVALUATION_PLAN_REQUEST_TYPE, PolicyEvaluationPlanRequestValidator.instance());

        var monitor = context.getMonitor();
        webService.registerResource(ApiContext.MANAGEMENT, new PolicyDefinitionApiV3Controller(monitor, managementApiTransformerRegistry, service, validatorRegistry));
        webService.registerResource(ApiContext.MANAGEMENT, new PolicyDefinitionApiV3Controller(monitor, managementApiTransformerRegistry, service, validatorRegistry));
    }
}
