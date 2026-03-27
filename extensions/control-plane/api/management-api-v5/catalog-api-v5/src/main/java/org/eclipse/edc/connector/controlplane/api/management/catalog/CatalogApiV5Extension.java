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

package org.eclipse.edc.connector.controlplane.api.management.catalog;

import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.controlplane.api.management.catalog.v5.CatalogApiV5Controller;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.transform.edc.catalog.to.JsonObjectToCatalogRequestTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.catalog.to.JsonObjectToDatasetRequestTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
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

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = CatalogApiV5Extension.NAME)
public class CatalogApiV5Extension implements ServiceExtension {

    public static final String NAME = "Management API: Catalog";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private CatalogService service;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;
    @Inject
    private AuthorizationService authorizationService;
    @Inject
    private ParticipantContextService participantContextService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new JsonObjectToCatalogRequestTransformer());
        transformerRegistry.register(new JsonObjectToDatasetRequestTransformer());

        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");

        // authorization service does need an additional lookup function - catalogs are not a persisted entity

        webService.registerResource(ApiContext.MANAGEMENT, new CatalogApiV5Controller(service, managementApiTransformerRegistry, authorizationService, participantContextService));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, CatalogApiV5Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V4.version()));
    }
}
