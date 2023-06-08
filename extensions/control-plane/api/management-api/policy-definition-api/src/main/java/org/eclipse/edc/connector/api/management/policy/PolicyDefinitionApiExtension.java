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

package org.eclipse.edc.connector.api.management.policy;

import jakarta.json.Json;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.api.management.policy.transform.JsonObjectFromPolicyDefinitionResponseDtoTransformer;
import org.eclipse.edc.connector.api.management.policy.transform.JsonObjectToPolicyDefinitionRequestDtoTransformer;
import org.eclipse.edc.connector.api.management.policy.transform.JsonObjectToPolicyDefinitionUpdateDtoTransformer;
import org.eclipse.edc.connector.api.management.policy.transform.PolicyDefinitionRequestDtoToPolicyDefinitionTransformer;
import org.eclipse.edc.connector.api.management.policy.transform.PolicyDefinitionToPolicyDefinitionResponseDtoTransformer;
import org.eclipse.edc.connector.api.management.policy.transform.PolicyDefinitionUpdateWrapperDtoToPolicyDefinitionTransformer;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;

import java.util.Map;


@Extension(value = PolicyDefinitionApiExtension.NAME)
public class PolicyDefinitionApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Policy";

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private WebService webService;

    @Inject
    private ManagementApiConfiguration configuration;

    @Inject
    private PolicyDefinitionService service;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jsonBuilderFactory = Json.createBuilderFactory(Map.of());
        transformerRegistry.register(new PolicyDefinitionRequestDtoToPolicyDefinitionTransformer());
        transformerRegistry.register(new PolicyDefinitionToPolicyDefinitionResponseDtoTransformer());
        transformerRegistry.register(new PolicyDefinitionUpdateWrapperDtoToPolicyDefinitionTransformer());
        transformerRegistry.register(new JsonObjectToPolicyDefinitionRequestDtoTransformer());
        transformerRegistry.register(new JsonObjectToPolicyDefinitionUpdateDtoTransformer());
        transformerRegistry.register(new JsonObjectFromPolicyDefinitionResponseDtoTransformer(jsonBuilderFactory));

        var monitor = context.getMonitor();
        webService.registerResource(configuration.getContextAlias(), new PolicyDefinitionApiController(monitor, transformerRegistry, service));
    }
}
