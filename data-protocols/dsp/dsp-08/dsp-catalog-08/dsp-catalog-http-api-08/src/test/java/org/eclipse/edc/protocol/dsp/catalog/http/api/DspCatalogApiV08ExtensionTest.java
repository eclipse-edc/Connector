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

package org.eclipse.edc.protocol.dsp.catalog.http.api;

import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.protocol.dsp.spi.transform.DspProtocolTypeTransformerRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_08;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DspCatalogApiV08ExtensionTest {

    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final ProtocolVersionRegistry versionRegistry = mock();
    private final DspProtocolTypeTransformerRegistry dspTransformerRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(JsonObjectValidatorRegistry.class, validatorRegistry);
        context.registerService(ProtocolVersionRegistry.class, versionRegistry);
        context.registerService(DspProtocolTypeTransformerRegistry.class, dspTransformerRegistry);

        when(dspTransformerRegistry.forProtocol(any())).thenReturn(Result.success(mock()));
    }

    @Test
    void shouldRegisterMessageValidator(DspCatalogApiV08Extension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(validatorRegistry).register(eq(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_IRI), any());
    }

    @Test
    void shouldRegisterDspVersion(DspCatalogApiV08Extension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(versionRegistry).register(V_08);
    }
}
