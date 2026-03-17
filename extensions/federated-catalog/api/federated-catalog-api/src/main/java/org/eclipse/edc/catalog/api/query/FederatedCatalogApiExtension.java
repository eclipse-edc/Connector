/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.catalog.api.query;

import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.catalog.api.query.v3.CatalogsApiV3Controller;
import org.eclipse.edc.catalog.api.query.v4.CatalogsApiV4Controller;
import org.eclipse.edc.catalog.spi.QueryService;
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

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_CONTEXT_2025_1;
import static org.eclipse.edc.jsonld.spi.Namespaces.EDC_DSPACE_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = FederatedCatalogApiExtension.NAME)
public class FederatedCatalogApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Federated Catalog";

    @Inject
    private WebService webService;
    @Inject
    private QueryService queryService;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");

        jsonLd.registerContext(DSPACE_CONTEXT_2025_1, MANAGEMENT_SCOPE);
        jsonLd.registerContext(EDC_DSPACE_CONTEXT, MANAGEMENT_SCOPE);

        webService.registerResource(ApiContext.MANAGEMENT, new CatalogsApiV3Controller(queryService, managementApiTransformerRegistry));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, CatalogsApiV3Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE));

        webService.registerResource(ApiContext.MANAGEMENT, new CatalogsApiV4Controller(queryService, managementApiTransformerRegistry));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, CatalogsApiV4Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V4.version()));
    }
}
