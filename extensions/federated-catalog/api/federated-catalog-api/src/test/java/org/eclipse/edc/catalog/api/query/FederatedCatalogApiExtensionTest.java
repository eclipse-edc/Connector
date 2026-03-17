/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.api.query;

import org.eclipse.edc.catalog.api.query.v3.CatalogsApiV3Controller;
import org.eclipse.edc.catalog.api.query.v4.CatalogsApiV4Controller;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_CONTEXT_2025_1;
import static org.eclipse.edc.jsonld.spi.Namespaces.EDC_DSPACE_CONTEXT;
import static org.eclipse.edc.web.spi.configuration.ApiContext.MANAGEMENT;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class FederatedCatalogApiExtensionTest {

    private final WebService webService = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final JsonLd jsonLd = mock();
    private final TypeManager typeManager = mock();
    private final QueryService queryService = mock();
    private final JsonObjectValidatorRegistry validatorRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(WebService.class, webService);
        context.registerService(TypeTransformerRegistry.class, transformerRegistry);
        context.registerService(JsonLd.class, jsonLd);
        context.registerService(TypeManager.class, typeManager);
        context.registerService(QueryService.class, queryService);
        context.registerService(JsonObjectValidatorRegistry.class, validatorRegistry);
        when(transformerRegistry.forContext("management-api")).thenReturn(transformerRegistry);
    }

    @Test
    void initialize_shouldRegisterV3AndV4ManagementApiResources(FederatedCatalogApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(webService).registerResource(eq(MANAGEMENT), isA(CatalogsApiV3Controller.class));
        verify(webService).registerDynamicResource(eq(MANAGEMENT), eq(CatalogsApiV3Controller.class), isA(JerseyJsonLdInterceptor.class));

        verify(webService).registerResource(eq(MANAGEMENT), isA(CatalogsApiV4Controller.class));
        verify(webService).registerDynamicResource(eq(MANAGEMENT), eq(CatalogsApiV4Controller.class), isA(JerseyJsonLdInterceptor.class));

        verify(jsonLd).registerContext(DSPACE_CONTEXT_2025_1, MANAGEMENT_SCOPE);
        verify(jsonLd).registerContext(EDC_DSPACE_CONTEXT, MANAGEMENT_SCOPE);
    }
}
