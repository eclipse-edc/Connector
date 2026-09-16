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

package org.eclipse.edc.connector.dataplane.selector;

import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.connector.controlplane.dataplane.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.api.v5.DataplaneSelectorApiV5Controller;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataPlaneInstanceTransformer;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;

import java.util.Map;

import static jakarta.json.Json.createBuilderFactory;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V5;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.eclipse.edc.web.spi.configuration.ApiContext.MANAGEMENT;

@Extension(value = DataPlaneSelectorApiV5Extension.NAME)
public class DataPlaneSelectorApiV5Extension implements ServiceExtension {

    public static final String NAME = "Management API: Dataplane Selector V5";

    @Inject
    private WebService webService;

    @Inject
    private DataPlaneSelectorService selectionService;

    @Inject
    private TypeManager typeManager;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private AuthorizationService authorizationService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var managementApiTransformerRegistry = transformerRegistry.forContext(MANAGEMENT_API_CONTEXT);
        managementApiTransformerRegistry.register(new JsonObjectFromDataPlaneInstanceTransformer(createBuilderFactory(Map.of()), typeManager, JSON_LD));

        webService.registerResource(MANAGEMENT, new DataplaneSelectorApiV5Controller(selectionService, managementApiTransformerRegistry,
                monitor, authorizationService));
        webService.registerDynamicResource(MANAGEMENT, DataplaneSelectorApiV5Controller.class,
                new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V5));
    }
}
