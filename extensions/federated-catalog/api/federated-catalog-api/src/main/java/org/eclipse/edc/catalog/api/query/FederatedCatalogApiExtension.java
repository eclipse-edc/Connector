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

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.io.IOException;
import java.util.stream.Stream;

import static org.eclipse.edc.catalog.spi.FccApiContexts.CATALOG_QUERY;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_CONTEXT_2025_1;
import static org.eclipse.edc.jsonld.spi.Namespaces.EDC_DSPACE_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = FederatedCatalogApiExtension.NAME)
public class FederatedCatalogApiExtension implements ServiceExtension {

    public static final String NAME = "Cache Query API Extension";
    static final String CATALOG_QUERY_SCOPE = "CATALOG_QUERY_API";
    private static final String API_VERSION_JSON_FILE = "catalog-version.json";

    @Configuration
    private CatalogApiConfiguration apiConfiguration;

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
    private ApiVersionService apiVersionService;
    @Inject
    private PortMappingRegistry portMappingRegistry;
    @Inject(required = false)
    private HealthCheckService healthCheckService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        portMappingRegistry.register(new PortMapping(CATALOG_QUERY, apiConfiguration.port(), apiConfiguration.path()));

        jsonLd.registerContext(DSPACE_CONTEXT_2025_1, CATALOG_QUERY_SCOPE);
        jsonLd.registerContext(EDC_DSPACE_CONTEXT, CATALOG_QUERY_SCOPE);

        webService.registerResource(CATALOG_QUERY, new FederatedCatalogApiController(queryService, transformerRegistry));
        webService.registerResource(CATALOG_QUERY, new ObjectMapperProvider(typeManager, JSON_LD));
        webService.registerResource(CATALOG_QUERY, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, CATALOG_QUERY_SCOPE));

        if (healthCheckService != null) {
            var successResult = HealthCheckResult.Builder.newInstance().component("FCC Query API").build();
            healthCheckService.addReadinessProvider(() -> successResult);
            healthCheckService.addLivenessProvider(() -> successResult);
        }

        registerVersionInfo(getClass().getClassLoader());
    }

    private void registerVersionInfo(ClassLoader resourceClassLoader) {
        try (var versionContent = resourceClassLoader.getResourceAsStream(API_VERSION_JSON_FILE)) {
            if (versionContent == null) {
                throw new EdcException("Version file not found or not readable.");
            }
            Stream.of(typeManager.getMapper()
                            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                            .readValue(versionContent, VersionRecord[].class))
                    .forEach(versionRecord -> apiVersionService.addRecord(CATALOG_QUERY, versionRecord));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Settings
    record CatalogApiConfiguration(
            @Setting(key = "web.http." + CATALOG_QUERY + ".port", description = "Port for " + CATALOG_QUERY + " api context", defaultValue = 17171 + "")
            int port,
            @Setting(key = "web.http." + CATALOG_QUERY + ".path", description = "Path for " + CATALOG_QUERY + " api context", defaultValue = "/api/catalog")
            String path
    ) {
    }
}
