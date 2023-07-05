/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.asset;

import org.eclipse.edc.connector.api.management.asset.v3.AssetApiController;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto.EDC_ASSET_ENTRY_DTO_TYPE;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE;
import static org.eclipse.edc.spi.types.domain.asset.Asset.EDC_ASSET_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class AssetApiExtensionTest {

    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final WebService webService = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(JsonObjectValidatorRegistry.class, validatorRegistry);
        context.registerService(WebService.class, webService);
    }

    @Test
    void initialize_shouldRegisterControllers(AssetApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(webService).registerResource(any(), isA(org.eclipse.edc.connector.api.management.asset.v2.AssetApiController.class));
        verify(webService).registerResource(any(), isA(AssetApiController.class));
    }

    @Test
    void initialize_shouldRegisterValidators(AssetApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(validatorRegistry).register(eq(EDC_ASSET_TYPE), any());
        verify(validatorRegistry).register(eq(EDC_ASSET_ENTRY_DTO_TYPE), any());
        verify(validatorRegistry).register(eq(EDC_DATA_ADDRESS_TYPE), any());
    }
}
