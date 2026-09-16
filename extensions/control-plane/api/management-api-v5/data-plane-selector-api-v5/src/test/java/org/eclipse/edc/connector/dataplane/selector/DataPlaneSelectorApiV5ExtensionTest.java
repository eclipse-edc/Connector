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

import org.eclipse.edc.connector.dataplane.selector.api.v5.DataplaneSelectorApiV5Controller;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataPlaneInstanceTransformer;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.web.spi.configuration.ApiContext.MANAGEMENT;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneSelectorApiV5ExtensionTest {

    private final WebService webService = mock();
    private final TypeTransformerRegistry managementApiTransformerRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        TypeTransformerRegistry parentTransformerRegistry = mock();
        when(parentTransformerRegistry.forContext(MANAGEMENT_API_CONTEXT)).thenReturn(managementApiTransformerRegistry);
        context.registerService(TypeTransformerRegistry.class, parentTransformerRegistry);
        context.registerService(WebService.class, webService);
    }

    @Test
    void shouldRegisterTransformer(DataPlaneSelectorApiV5Extension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(managementApiTransformerRegistry).register(isA(JsonObjectFromDataPlaneInstanceTransformer.class));
    }

    @Test
    void shouldRegisterController(DataPlaneSelectorApiV5Extension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(webService).registerResource(eq(MANAGEMENT), isA(DataplaneSelectorApiV5Controller.class));
        verify(webService).registerDynamicResource(eq(MANAGEMENT), eq(DataplaneSelectorApiV5Controller.class), isA(Object.class));
    }
}
