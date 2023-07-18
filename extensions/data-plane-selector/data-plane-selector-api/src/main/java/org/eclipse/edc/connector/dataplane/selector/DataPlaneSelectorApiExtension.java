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

import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.dataplane.selector.api.DataplaneSelectorApiController;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.transformer.JsonObjectFromDataPlaneInstanceTransformer;
import org.eclipse.edc.connector.dataplane.selector.transformer.JsonObjectToDataPlaneInstanceTransformer;
import org.eclipse.edc.connector.dataplane.selector.transformer.JsonObjectToSelectionRequestTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;

import java.util.Map;

import static jakarta.json.Json.createBuilderFactory;
import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

@Extension(value = "DataPlane selector API")
public class DataPlaneSelectorApiExtension implements ServiceExtension {

    @Inject
    private WebService webservice;

    @Inject
    private DataPlaneSelectorService selectionService;

    @Inject
    private ManagementApiConfiguration managementApiConfiguration;

    @Inject
    private TypeManager typeManager;
    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        //todo: add authentication
        //var filter = new AuthenticationRequestFilter();

        typeManager.registerTypes(DataPlaneInstance.class);

        transformerRegistry.register(new JsonObjectToSelectionRequestTransformer());
        transformerRegistry.register(new JsonObjectToDataPlaneInstanceTransformer());
        transformerRegistry.register(new JsonObjectFromDataPlaneInstanceTransformer(createBuilderFactory(Map.of()), typeManager.getMapper(JSON_LD)));
        var controller = new DataplaneSelectorApiController(selectionService, transformerRegistry);

        webservice.registerResource(managementApiConfiguration.getContextAlias(), controller);
    }
}
