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

package org.eclipse.edc.connector.controlplane.api.management.protocolversion;

import org.eclipse.edc.connector.controlplane.api.management.protocolversion.transform.JsonObjectToProtocolVersionRequestTransformer;
import org.eclipse.edc.connector.controlplane.api.management.protocolversion.v4alpha.ProtocolVersionApiV4AlphaController;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest.PROTOCOL_VERSION_REQUEST_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class ProtocolVersionApiExtensionTest {

    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final WebService webService = mock(WebService.class);
    private final TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);
    private final TypeTransformerRegistry managementApiTransformer = mock(TypeTransformerRegistry.class);

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(JsonObjectValidatorRegistry.class, validatorRegistry);
        context.registerService(WebService.class, webService);
        context.registerService(TypeTransformerRegistry.class, transformerRegistry);

        when(transformerRegistry.forContext(MANAGEMENT_API_CONTEXT)).thenReturn(managementApiTransformer);
    }

    @Test
    void initiate_shouldRegisterValidators(ProtocolVersionApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(validatorRegistry).register(eq(PROTOCOL_VERSION_REQUEST_TYPE), any());
    }

    @Test
    void initiate_shouldRegisterControllers(ProtocolVersionApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(webService).registerResource(any(), isA(ProtocolVersionApiV4AlphaController.class));
    }


    @Test
    void initiate_shouldRegisterTransformers(ProtocolVersionApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(managementApiTransformer).register(isA(JsonObjectToProtocolVersionRequestTransformer.class));
    }
}
