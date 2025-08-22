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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       SAP SE - add private properties to contract definition
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.contractdefinition;

import jakarta.json.Json;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.controlplane.api.management.contractdefinition.transform.JsonObjectFromContractDefinitionTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractdefinition.transform.JsonObjectToContractDefinitionTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractdefinition.v3.ContractDefinitionApiV3Controller;
import org.eclipse.edc.connector.controlplane.api.management.contractdefinition.v4.ContractDefinitionApiV4Controller;
import org.eclipse.edc.connector.controlplane.api.management.contractdefinition.validation.ContractDefinitionValidator;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = ContractDefinitionApiExtension.NAME)
public class ContractDefinitionApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Contract Definition";

    @Inject
    WebService webService;

    @Inject
    TypeTransformerRegistry transformerRegistry;

    @Inject
    ContractDefinitionService service;

    @Inject
    JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private TypeManager typeManager;

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Inject
    private JsonLd jsonLd;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jsonFactory = Json.createBuilderFactory(Map.of());
        transformerRegistry.register(new JsonObjectFromContractDefinitionTransformer(jsonFactory, typeManager, JSON_LD));
        transformerRegistry.register(new JsonObjectToContractDefinitionTransformer());

        validatorRegistry.register(CONTRACT_DEFINITION_TYPE, ContractDefinitionValidator.instance(criterionOperatorRegistry));

        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");

        webService.registerResource(ApiContext.MANAGEMENT, new ContractDefinitionApiV3Controller(managementApiTransformerRegistry, service, context.getMonitor(), validatorRegistry));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, ContractDefinitionApiV3Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE));

        webService.registerResource(ApiContext.MANAGEMENT, new ContractDefinitionApiV4Controller(managementApiTransformerRegistry, service, context.getMonitor(), validatorRegistry));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, ContractDefinitionApiV4Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V4.version()));

    }
}
