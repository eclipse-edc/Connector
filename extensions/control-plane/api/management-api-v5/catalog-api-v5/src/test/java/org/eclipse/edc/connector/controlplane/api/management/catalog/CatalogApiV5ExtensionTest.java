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

package org.eclipse.edc.connector.controlplane.api.management.catalog;

import org.eclipse.edc.connector.controlplane.api.management.catalog.v5.CatalogApiV5Controller;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class CatalogApiV5ExtensionTest {

    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final WebService webService = mock(WebService.class);

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(JsonObjectValidatorRegistry.class, validatorRegistry);
        context.registerService(WebService.class, webService);
    }

    @Test
    void initiate_shouldRegisterControllers(CatalogApiV5Extension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(webService).registerResource(any(), isA(CatalogApiV5Controller.class));
    }
}
