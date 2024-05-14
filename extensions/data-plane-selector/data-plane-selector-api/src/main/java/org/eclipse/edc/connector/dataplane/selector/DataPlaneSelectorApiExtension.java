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
import org.eclipse.edc.connector.dataplane.selector.api.v2.DataplaneSelectorApiController;
import org.eclipse.edc.connector.dataplane.selector.api.v2.validation.DataPlaneInstanceValidator;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.transformer.JsonObjectToSelectionRequestTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataPlaneInstanceTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataPlaneInstanceTransformer;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;

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
    private ManagementApiConfiguration managementApiConfiguration;

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
        typeManager.registerTypes(DataPlaneInstance.class);
        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");

        validatorRegistry.register(DATAPLANE_INSTANCE_TYPE, DataPlaneInstanceValidator.instance());
        managementApiTransformerRegistry.register(new JsonObjectToSelectionRequestTransformer());
        managementApiTransformerRegistry.register(new JsonObjectToDataPlaneInstanceTransformer());
        managementApiTransformerRegistry.register(new JsonObjectFromDataPlaneInstanceTransformer(createBuilderFactory(Map.of()), typeManager.getMapper(JSON_LD)));
        var controller = new DataplaneSelectorApiController(selectionService, managementApiTransformerRegistry, validatorRegistry, clock);

        webservice.registerResource(managementApiConfiguration.getContextAlias(), controller);
    }
}
