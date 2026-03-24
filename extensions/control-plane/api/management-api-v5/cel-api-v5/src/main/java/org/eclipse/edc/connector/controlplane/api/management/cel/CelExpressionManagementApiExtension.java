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

package org.eclipse.edc.connector.controlplane.api.management.cel;

import jakarta.json.Json;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.controlplane.api.management.cel.v5.CelExpressionApiV5Controller;
import org.eclipse.edc.connector.controlplane.transform.edc.cel.from.JsonObjectFromCelExpressionTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.cel.to.JsonObjectToCelExpressionTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.policy.cel.service.CelPolicyExpressionService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
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
import static org.eclipse.edc.connector.controlplane.api.management.cel.CelExpressionManagementApiExtension.NAME;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class CelExpressionManagementApiExtension implements ServiceExtension {

    public static final String NAME = "CEL management API Extension";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;

    @Inject
    private CelPolicyExpressionService service;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var factory = Json.createBuilderFactory(Map.of());

        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");

        managementApiTransformerRegistry.register(new JsonObjectFromCelExpressionTransformer(factory));
        managementApiTransformerRegistry.register(new JsonObjectToCelExpressionTransformer());

        webService.registerResource(ApiContext.MANAGEMENT, new CelExpressionApiV5Controller(service, managementApiTransformerRegistry));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, CelExpressionApiV5Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V4.version()));

    }
}
