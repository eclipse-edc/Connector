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

package org.eclipse.edc.connector.dataplane.selector;

import org.eclipse.edc.connector.dataplane.selector.api.v3.DataplaneSelectorApiV3Controller;
import org.eclipse.edc.connector.dataplane.selector.api.v4.DataplaneSelectorApiV4Controller;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataPlaneInstanceTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataPlaneInstanceV3Transformer;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static jakarta.json.Json.createBuilderFactory;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = "DataPlane selector API")
public class DataPlaneSelectorApiExtension implements ServiceExtension {

    @Inject
    private WebService webservice;

    @Inject
    private DataPlaneSelectorService selectionService;

    @Inject
    private TypeManager typeManager;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");

        // V4
        managementApiTransformerRegistry.register(new JsonObjectFromDataPlaneInstanceTransformer(createBuilderFactory(Map.of()), typeManager, JSON_LD));
        webservice.registerResource(ApiContext.MANAGEMENT, new DataplaneSelectorApiV4Controller(selectionService, managementApiTransformerRegistry));
        webservice.registerDynamicResource(ApiContext.MANAGEMENT, DataplaneSelectorApiV4Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE));

        // V3
        var managementApiTransformerRegistryV3 = managementApiTransformerRegistry.forContext("v3");
        managementApiTransformerRegistryV3.register(new JsonObjectFromDataPlaneInstanceV3Transformer(createBuilderFactory(Map.of()), typeManager, JSON_LD));
        webservice.registerResource(ApiContext.MANAGEMENT, new DataplaneSelectorApiV3Controller(selectionService, managementApiTransformerRegistryV3));

        webservice.registerDynamicResource(ApiContext.MANAGEMENT, DataplaneSelectorApiV3Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE));

    }
}
