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

import org.eclipse.edc.connector.dataplane.selector.api.v2.DataplaneSelectorApiV2Controller;
import org.eclipse.edc.connector.dataplane.selector.api.v3.DataplaneSelectorApiV3Controller;
import org.eclipse.edc.connector.dataplane.selector.api.v4.DataplaneSelectorApiV4Controller;
import org.eclipse.edc.connector.dataplane.selector.api.validation.DataPlaneInstanceValidator;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.transformer.JsonObjectToSelectionRequestTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataPlaneInstanceTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataPlaneInstanceV3Transformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataPlaneInstanceTransformer;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.time.Clock;
import java.util.Map;

import static jakarta.json.Json.createBuilderFactory;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.DATAPLANE_INSTANCE_TYPE;
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
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private Clock clock;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");

        // V4
        managementApiTransformerRegistry.register(new JsonObjectFromDataPlaneInstanceTransformer(createBuilderFactory(Map.of()), typeManager.getMapper(JSON_LD)));
        webservice.registerResource(ApiContext.MANAGEMENT, new DataplaneSelectorApiV4Controller(selectionService, managementApiTransformerRegistry));

        // V3
        var managementApiTransformerRegistryV3 = managementApiTransformerRegistry.forContext("v3");
        managementApiTransformerRegistryV3.register(new JsonObjectFromDataPlaneInstanceV3Transformer(createBuilderFactory(Map.of()), typeManager.getMapper(JSON_LD)));
        webservice.registerResource(ApiContext.MANAGEMENT, new DataplaneSelectorApiV3Controller(selectionService, managementApiTransformerRegistryV3));

        // V2
        validatorRegistry.register(DATAPLANE_INSTANCE_TYPE, DataPlaneInstanceValidator.instance());
        var managementApiTransformerRegistryV2 = managementApiTransformerRegistry.forContext("v2");
        managementApiTransformerRegistryV2.register(new JsonObjectToSelectionRequestTransformer());
        managementApiTransformerRegistryV2.register(new JsonObjectToDataPlaneInstanceTransformer());
        managementApiTransformerRegistryV2.register(new JsonObjectFromDataPlaneInstanceV3Transformer(createBuilderFactory(Map.of()), typeManager.getMapper(JSON_LD)));
        webservice.registerResource(ApiContext.MANAGEMENT, new DataplaneSelectorApiV2Controller(selectionService, managementApiTransformerRegistryV2, validatorRegistry, clock, context.getMonitor()));
    }
}
