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

package org.eclipse.edc.connector.controlplane.api.management.contractdefinition;

import jakarta.json.Json;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.controlplane.api.management.contractdefinition.v5.ContractDefinitionApiV5Controller;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.controlplane.transform.edc.contractdefinition.from.JsonObjectFromContractDefinitionTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.contractdefinition.to.JsonObjectToContractDefinitionTransformer;
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

@Extension(value = ContractDefinitionApiV5Extension.NAME)
public class ContractDefinitionApiV5Extension implements ServiceExtension {

    public static final String NAME = "Management API: Contract Definition";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private ContractDefinitionService contractDefinitionService;

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
        var jsonFactory = Json.createBuilderFactory(Map.of());
        transformerRegistry.register(new JsonObjectFromContractDefinitionTransformer(jsonFactory, typeManager, JSON_LD));
        transformerRegistry.register(new JsonObjectToContractDefinitionTransformer());

        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");
        authorizationService.addLookupFunction(ContractDefinition.class, this::findContractDef);
        webService.registerResource(ApiContext.MANAGEMENT, new ContractDefinitionApiV5Controller(managementApiTransformerRegistry, contractDefinitionService, context.getMonitor(), authorizationService));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, ContractDefinitionApiV5Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V4.version()));

    }

    private ParticipantResource findContractDef(String ownerId, String assetId) {
        return contractDefinitionService
                .search(QuerySpec.Builder.newInstance()
                        .filter(new Criterion("participantContextId", "=", ownerId))
                        .filter(new Criterion("id", "=", assetId))
                        .build()
                )
                .map(it -> it.stream().findFirst().orElse(null))
                .orElse(f -> null);
    }

}
