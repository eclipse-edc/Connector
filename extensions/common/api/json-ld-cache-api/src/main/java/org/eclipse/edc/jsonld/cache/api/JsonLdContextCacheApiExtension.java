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

package org.eclipse.edc.jsonld.cache.api;

import jakarta.json.Json;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.jsonld.cache.api.v5.JsonLdContextCacheApiV5Controller;
import org.eclipse.edc.jsonld.cache.api.v5.transform.JsonObjectFromCachedJsonLdContextTransformer;
import org.eclipse.edc.jsonld.cache.api.v5.transform.JsonObjectToCachedJsonLdContextTransformer;
import org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContextService;
import org.eclipse.edc.jsonld.spi.JsonLd;
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

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = JsonLdContextCacheApiExtension.NAME)
public class JsonLdContextCacheApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: JSON-LD Context Cache";

    @Inject
    private WebService webService;
    @Inject
    private CachedJsonLdContextService service;
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
        managementApiTransformerRegistry.register(new JsonObjectToCachedJsonLdContextTransformer());
        managementApiTransformerRegistry.register(new JsonObjectFromCachedJsonLdContextTransformer(factory));

        webService.registerResource(ApiContext.MANAGEMENT,
                new JsonLdContextCacheApiV5Controller(service, managementApiTransformerRegistry));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, JsonLdContextCacheApiV5Controller.class,
                new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V5.version()));
    }
}
