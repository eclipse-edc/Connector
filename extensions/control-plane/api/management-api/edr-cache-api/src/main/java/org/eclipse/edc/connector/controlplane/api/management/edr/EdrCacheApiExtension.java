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

package org.eclipse.edc.connector.controlplane.api.management.edr;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.api.management.edr.transform.JsonObjectFromEndpointDataReferenceEntryTransformer;
import org.eclipse.edc.connector.controlplane.api.management.edr.v3.EdrCacheApiV3Controller;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE;
import static org.eclipse.edc.connector.controlplane.api.management.edr.EdrCacheApiExtension.NAME;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(NAME)
public class EdrCacheApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: EDR cache";

    @Inject
    private WebService webService;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private JsonObjectValidatorRegistry validator;
    @Inject
    private EndpointDataReferenceStore edrStore;
    @Inject
    private Monitor monitor;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jsonFactory = Json.createBuilderFactory(Map.of());
        var managementTypeTransformerRegistry = transformerRegistry.forContext("management-api");

        managementTypeTransformerRegistry.register(new JsonObjectFromEndpointDataReferenceEntryTransformer(jsonFactory));

        webService.registerResource(ApiContext.MANAGEMENT, new EdrCacheApiV3Controller(edrStore, managementTypeTransformerRegistry, validator, monitor));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, EdrCacheApiV3Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE));

    }
}
