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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - register missing transformer
 *
 */

package org.eclipse.edc.connector.dataplane.selector.control.api;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataPlaneInstanceTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataPlaneInstanceTransformer;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DataplaneSelectorControlApiExtensionTest {

    private final WebService webService = mock();
    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(WebService.class, webService);
        context.registerService(JsonObjectValidatorRegistry.class, validatorRegistry);
        context.registerService(TypeTransformerRegistry.class, typeTransformerRegistry);
    }

    @Test
    void shouldRegisterController(DataplaneSelectorControlApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(webService).registerResource(eq(ApiContext.CONTROL), isA(DataplaneSelectorControlApiController.class));
    }

    @Test
    void shouldRegisterValidator(DataplaneSelectorControlApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(validatorRegistry).register(eq(DataPlaneInstance.DATAPLANE_INSTANCE_TYPE), isA(JsonObjectValidator.class));
    }

    @Test
    void shouldRegisterTransformers(DataplaneSelectorControlApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(typeTransformerRegistry).register(isA(JsonObjectToDataPlaneInstanceTransformer.class));
        verify(typeTransformerRegistry).register(isA(JsonObjectFromDataPlaneInstanceTransformer.class));
    }
}
