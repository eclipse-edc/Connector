/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Contributors to the Eclipse Foundation - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.api.configuration.v2025.virtual;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_HTTP_PROFILE_2025_1;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DspVirtualApiConfigurationV2025ExtensionTest {

    private final DspBaseWebhookAddress webhookAddress = mock();
    private final DataspaceProfileContextRegistry registry = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        when(transformerRegistry.forContext(any())).thenReturn(mock());
        context.registerService(DspBaseWebhookAddress.class, webhookAddress);
        context.registerService(DataspaceProfileContextRegistry.class, registry);
        context.registerService(TypeTransformerRegistry.class, transformerRegistry);
        context.registerService(TypeManager.class, mock());
        context.registerService(JsonLd.class, mock());
        context.registerService(ParticipantIdMapper.class, mock());
        context.registerService(DefaultParticipantIdExtractionFunction.class, mock());
        context.registerService(CriterionOperatorRegistry.class, mock());
    }

    @Test
    void initialize_shouldRegisterDefaultProfile_whenWebhookContainsParticipantPlaceholder(DspVirtualApiConfigurationV2025Extension extension, ServiceExtensionContext context) {
        when(webhookAddress.get()).thenReturn("http://localhost:8282/protocol/%s");

        extension.initialize(context);

        verify(registry).registerDefault(argThat(profile -> profile.webhook().url().equals("http://localhost:8282/protocol/%s/" + DATASPACE_HTTP_PROFILE_2025_1)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "http://localhost:8282/protocol", "http://localhost:8282/protocol/" })
    void initialize_shouldFail_whenWebhookLacksParticipantPlaceholder(String address, DspVirtualApiConfigurationV2025Extension extension, ServiceExtensionContext context) {
        when(webhookAddress.get()).thenReturn(address);

        assertThatThrownBy(() -> extension.initialize(context))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("edc.dsp.callback.address")
                .hasMessageContaining("web.http.protocol.virtual");

        verify(registry, never()).registerDefault(any());
    }
}
