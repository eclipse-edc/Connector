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

import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.api.management.policy.transform.PolicyDefinitionRequestDtoToPolicyDefinitionTransformer;
import org.eclipse.edc.connector.api.management.policy.transform.PolicyDefinitionToPolicyDefinitionResponseDtoTransformer;
import org.eclipse.edc.connector.api.management.policy.transform.PolicyDefinitionUpdateWrapperDtoToPolicyDefinitionTransformer;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;

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

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new PolicyDefinitionRequestDtoToPolicyDefinitionTransformer());
        transformerRegistry.register(new PolicyDefinitionToPolicyDefinitionResponseDtoTransformer());
        transformerRegistry.register(new PolicyDefinitionUpdateWrapperDtoToPolicyDefinitionTransformer());

        var monitor = context.getMonitor();
        var jsonLdMapper = typeManager.getMapper("json-ld");
        webService.registerResource(configuration.getContextAlias(), new ObjectMapperProvider(jsonLdMapper));
        webService.registerResource(configuration.getContextAlias(), new PolicyDefinitionApiController(monitor, service, transformerRegistry));
        webService.registerResource(configuration.getContextAlias(), new PolicyDefinitionNewApiController(monitor, transformerRegistry, service));
    }
}
