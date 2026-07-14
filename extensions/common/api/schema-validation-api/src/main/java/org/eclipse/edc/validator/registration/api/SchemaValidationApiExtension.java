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

package org.eclipse.edc.validator.registration.api;

import jakarta.json.Json;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.registration.api.v5.SchemaValidatorRegistrationApiV5Controller;
import org.eclipse.edc.validator.registration.api.v5.transform.JsonObjectFromSchemaValidatorRegistrationTransformer;
import org.eclipse.edc.validator.registration.api.v5.transform.JsonObjectToSchemaValidatorRegistrationTransformer;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistrationService;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = SchemaValidationApiExtension.NAME)
public class SchemaValidationApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Schema Validator Registration";

    @Inject
    private WebService webService;
    @Inject
    private SchemaValidatorRegistrationService service;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var factory = Json.createBuilderFactory(Map.of());
        var managementApiTransformerRegistry = transformerRegistry.forContext(MANAGEMENT_API_CONTEXT);
        managementApiTransformerRegistry.register(new JsonObjectToSchemaValidatorRegistrationTransformer());
        managementApiTransformerRegistry.register(new JsonObjectFromSchemaValidatorRegistrationTransformer(factory));

        webService.registerResource(ApiContext.MANAGEMENT,
                new SchemaValidatorRegistrationApiV5Controller(service, managementApiTransformerRegistry));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, SchemaValidatorRegistrationApiV5Controller.class,
                new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V5.version()));
    }
}
