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

package org.eclipse.edc.connector.controlplane.api.management.policy;

import jakarta.json.Json;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.controlplane.api.management.policy.v5.PolicyDefinitionApiV5Controller;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.connector.controlplane.transform.edc.policy.from.JsonObjectFromPolicyDefinitionTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.policy.from.JsonObjectFromPolicyEvaluationPlanTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.policy.from.JsonObjectFromPolicyValidationResultTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.policy.to.JsonObjectToPolicyDefinitionTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.policy.to.JsonObjectToPolicyEvaluationPlanRequestTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.types.ParticipantResource;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = PolicyDefinitionApiV5Extension.NAME)
public class PolicyDefinitionApiV5Extension implements ServiceExtension {

    public static final String NAME = "Management API: Policy Definition";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private PolicyDefinitionService policyDefinitionService;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;
    @Inject
    private AuthorizationService authorizationService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var jsonBuilderFactory = Json.createBuilderFactory(Map.of());

        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");
        managementApiTransformerRegistry.register(new JsonObjectToPolicyEvaluationPlanRequestTransformer());
        managementApiTransformerRegistry.register(new JsonObjectToPolicyDefinitionTransformer());
        managementApiTransformerRegistry.register(new JsonObjectFromPolicyDefinitionTransformer(jsonBuilderFactory, typeManager, JSON_LD));
        managementApiTransformerRegistry.register(new JsonObjectFromPolicyValidationResultTransformer(jsonBuilderFactory));
        managementApiTransformerRegistry.register(new JsonObjectFromPolicyEvaluationPlanTransformer(jsonBuilderFactory));

        authorizationService.addLookupFunction(PolicyDefinition.class, this::findPolicyDefinition);
        webService.registerResource(ApiContext.MANAGEMENT, new PolicyDefinitionApiV5Controller(policyDefinitionService, managementApiTransformerRegistry, monitor, authorizationService));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, PolicyDefinitionApiV5Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V4.version()));

    }

    private ParticipantResource findPolicyDefinition(String ownerId, String policyDefId) {
        return policyDefinitionService
                .search(QuerySpec.Builder.newInstance()
                        .filter(new Criterion("participantContextId", "=", ownerId))
                        .filter(new Criterion("id", "=", policyDefId))
                        .build()
                )
                .map(it -> it.stream().findFirst().orElse(null))
                .orElse(f -> null);
    }

}
