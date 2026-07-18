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

package org.eclipse.edc.connector.controlplane.api.management.dcpscope;

import jakarta.json.Json;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.controlplane.api.management.dcpscope.transform.JsonObjectFromDcpScopeTransformer;
import org.eclipse.edc.connector.controlplane.api.management.dcpscope.transform.JsonObjectToDcpScopeTransformer;
import org.eclipse.edc.connector.controlplane.api.management.dcpscope.v5.DcpScopeApiV5Controller;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
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

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.connector.controlplane.api.management.dcpscope.DcpScopeApiV5Extension.NAME;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class DcpScopeApiV5Extension implements ServiceExtension {

    public static final String NAME = "DcpScope Management API Extension";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;

    @Inject
    private DcpScopeRegistry scopeRegistry;

    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var factory = Json.createBuilderFactory(Map.of());

        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");

        managementApiTransformerRegistry.register(new JsonObjectToDcpScopeTransformer());
        managementApiTransformerRegistry.register(new JsonObjectFromDcpScopeTransformer(factory));

        webService.registerResource(ApiContext.MANAGEMENT, new DcpScopeApiV5Controller(scopeRegistry, managementApiTransformerRegistry, monitor));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, DcpScopeApiV5Controller.class,
                new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validatorRegistry, ManagementApiJsonSchema.V5.version()));
    }
}
