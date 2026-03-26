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

package org.eclipse.edc.protocol.dsp.metadata.http.api;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.protocol.dsp.version.transformer.JsonObjectFromProtocolVersionsTransformer;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
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
class DspMetadataApiExtensionTest {

    private final WebService webService = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(WebService.class, webService);
        context.registerService(TypeTransformerRegistry.class, transformerRegistry);
    }

    @Test
    void shouldRegisterApiController(DspMetadataApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(webService).registerResource(eq(ApiContext.PROTOCOL), isA(DspMetadataApiController.class));
        verify(transformerRegistry).register(isA(JsonObjectFromProtocolVersionsTransformer.class));
    }
}
