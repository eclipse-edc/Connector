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

package org.eclipse.edc.connector.dataplane.selector.control.api;

import org.eclipse.edc.connector.api.control.configuration.ControlApiConfiguration;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataPlaneInstanceTransformer;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;

import java.time.Clock;

import static org.eclipse.edc.connector.dataplane.selector.control.api.DataplaneSelectorControlApiExtension.NAME;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.DATAPLANE_INSTANCE_TYPE;

@Extension(NAME)
public class DataplaneSelectorControlApiExtension implements ServiceExtension {

    public static final String NAME = "Dataplane Selector Control API";

    @Inject
    private WebService webService;

    @Inject
    private ControlApiConfiguration controlApiConfiguration;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

    @Inject
    private DataPlaneSelectorService dataPlaneSelectorService;

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        validatorRegistry.register(DATAPLANE_INSTANCE_TYPE, DataPlaneInstanceValidator.instance());

        typeTransformerRegistry.register(new JsonObjectToDataPlaneInstanceTransformer());

        var controller = new DataplaneSelectorControlApiController(validatorRegistry, typeTransformerRegistry, dataPlaneSelectorService, clock);
        webService.registerResource(controlApiConfiguration.getContextAlias(), controller);
    }
}
