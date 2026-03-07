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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.catalog.api.query.FederatedCatalogApiExtension.CATALOG_QUERY_SCOPE;
import static org.eclipse.edc.catalog.spi.FccApiContexts.CATALOG_QUERY;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_CONTEXT_2025_1;
import static org.eclipse.edc.jsonld.spi.Namespaces.EDC_DSPACE_CONTEXT;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class FederatedCatalogApiExtensionTest {

    private final WebService webService = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final JsonLd jsonLd = mock();
    private final TypeManager typeManager = mock();
    private final QueryService queryService = mock();
    private final ApiVersionService apiVersionService = mock();
    private final PortMappingRegistry portMappingRegistry = mock();
    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(WebService.class, webService);
        context.registerService(TypeTransformerRegistry.class, transformerRegistry);
        context.registerService(JsonLd.class, jsonLd);
        context.registerService(TypeManager.class, typeManager);
        context.registerService(QueryService.class, queryService);
        context.registerService(ApiVersionService.class, apiVersionService);
        context.registerService(PortMappingRegistry.class, portMappingRegistry);
        when(typeManager.getMapper()).thenReturn(mapper);
    }

    @Test
    void initialize_shouldRegisterStandaloneCatalogApiResources(FederatedCatalogApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(portMappingRegistry).register(argThat(mapping ->
                mapping.name().equals(CATALOG_QUERY) && mapping.port() == 17171 && mapping.path().equals("/api/catalog")));
        verify(webService).registerResource(eq(CATALOG_QUERY), isA(FederatedCatalogApiController.class));
        verify(webService).registerResource(eq(CATALOG_QUERY), isA(ObjectMapperProvider.class));
        verify(webService).registerResource(eq(CATALOG_QUERY), isA(JerseyJsonLdInterceptor.class));
        verify(apiVersionService).addRecord(eq(CATALOG_QUERY), argThat(this::isCatalogApiVersion));
        verifyNoInteractions(transformerRegistry);
    }

    @Test
    void initialize_shouldRegisterNamespaces(FederatedCatalogApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(jsonLd).registerContext(DSPACE_CONTEXT_2025_1, CATALOG_QUERY_SCOPE);
        verify(jsonLd).registerContext(EDC_DSPACE_CONTEXT, CATALOG_QUERY_SCOPE);
    }

    private boolean isCatalogApiVersion(VersionRecord versionRecord) {
        return versionRecord != null &&
                "1.0.0-alpha".equals(versionRecord.version()) &&
                "/v1alpha".equals(versionRecord.urlPath()) &&
                versionRecord.maturity() == null;
    }
}
