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

package org.eclipse.edc.protocol.dsp.version.http.api;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.protocol.dsp.http.spi.configuration.DspApiConfiguration;
import org.eclipse.edc.protocol.dsp.version.http.api.transformer.JsonObjectFromProtocolVersionsTransformer;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DspVersionApiExtensionTest {

    private final WebService webService = mock();
    private final DspApiConfiguration dspApiConfiguration = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(WebService.class, webService);
        context.registerService(DspApiConfiguration.class, dspApiConfiguration);
        context.registerService(TypeTransformerRegistry.class, transformerRegistry);
    }

    @Test
    void shouldRegisterApiController(DspVersionApiExtension extension, ServiceExtensionContext context) {
        when(dspApiConfiguration.getContextAlias()).thenReturn("context-alias");

        extension.initialize(context);

        verify(webService).registerResource(eq("context-alias"), isA(DspVersionApiController.class));
        verify(transformerRegistry).register(isA(JsonObjectFromProtocolVersionsTransformer.class));
    }
}
