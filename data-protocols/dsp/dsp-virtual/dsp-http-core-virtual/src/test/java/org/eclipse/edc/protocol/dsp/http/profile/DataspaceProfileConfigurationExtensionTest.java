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

package org.eclipse.edc.protocol.dsp.http.profile;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DataspaceProfileConfigurationExtensionTest {

    private static final String BASE_WEBHOOK = "http://localhost:8080/protocol";

    private final DataspaceProfileContextRegistry registry = mock();
    private final DspBaseWebhookAddress webhookAddress = mock();
    private final DefaultParticipantIdExtractionFunction idExtractionFunction = mock();

    @BeforeEach
    void setup(TestExtensionContext context) {
        when(webhookAddress.get()).thenReturn(BASE_WEBHOOK);
        context.registerService(DataspaceProfileContextRegistry.class, registry);
        context.registerService(DspBaseWebhookAddress.class, webhookAddress);
        context.registerService(DefaultParticipantIdExtractionFunction.class, idExtractionFunction);
    }

    @Test
    void prepare_registersProfileFromConfiguration(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.dataspace.profiles.dsp2025_1.name", "dsp2025_1",
                "edc.dataspace.profiles.dsp2025_1.protocol.version", "2025-1",
                "edc.dataspace.profiles.dsp2025_1.protocol.binding", "HTTPS",
                "edc.dataspace.profiles.dsp2025_1.protocol.namespace", "https://w3id.org/dspace/v1",
                "edc.dataspace.profiles.dsp2025_1.jsonld.context.urls", "https://w3id.org/dspace/v1/context.jsonld , https://example.org/extra.jsonld"
        )));

        var extension = factory.constructInstance(DataspaceProfileConfigurationExtension.class);
        extension.initialize(context);

        var captor = ArgumentCaptor.forClass(DataspaceProfileContext.class);
        verify(registry).register(captor.capture());

        var registered = captor.getValue();
        assertThat(registered.name()).isEqualTo("dsp2025_1");
        assertThat(registered.protocolVersion()).isEqualTo(new ProtocolVersion("2025-1", "/dsp2025_1", "HTTPS"));
        assertThat(registered.protocolNamespace()).isEqualTo(new JsonLdNamespace("https://w3id.org/dspace/v1"));
        assertThat(registered.idExtractionFunction()).isSameAs(idExtractionFunction);
        assertThat(registered.webhook().url()).isEqualTo(BASE_WEBHOOK + "/dsp2025_1");
        assertThat(registered.jsonLdContextsUrl())
                .containsExactly("https://w3id.org/dspace/v1/context.jsonld", "https://example.org/extra.jsonld");
    }

    @Test
    void prepare_registersMultipleProfiles(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.dataspace.profiles.dsp2025_1.name", "dsp2025_1",
                "edc.dataspace.profiles.dsp2025_1.protocol.version", "2025-1",
                "edc.dataspace.profiles.dsp2025_1.protocol.binding", "HTTPS",
                "edc.dataspace.profiles.dsp2025_1.protocol.namespace", "https://w3id.org/dspace/2025/1/",
                "edc.dataspace.profiles.dsp2025_1.jsonld.context.urls", "https://w3id.org/dspace/2025/1/context.jsonld",
                "edc.dataspace.profiles.dsp2025_2.name", "dsp2025_2",
                "edc.dataspace.profiles.dsp2025_2.protocol.version", "2025-1",
                "edc.dataspace.profiles.dsp2025_2.protocol.binding", "HTTPS",
                "edc.dataspace.profiles.dsp2025_2.protocol.namespace", "https://w3id.org/dspace/2025/1/",
                "edc.dataspace.profiles.dsp2025_2.jsonld.context.urls", "https://w3id.org/dspace/2025/1/context.jsonld"
        )));

        var extension = factory.constructInstance(DataspaceProfileConfigurationExtension.class);
        extension.initialize(context);

        verify(registry, times(2)).register(ArgumentCaptor.forClass(DataspaceProfileContext.class).capture());
    }

    @Test
    void prepare_blankAndExtraCommasInJsonLdContextsAreFiltered(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.dataspace.profiles.dsp2025_1.name", "dsp2025_1",
                "edc.dataspace.profiles.dsp2025_1.protocol.version", "2025-1",
                "edc.dataspace.profiles.dsp2025_1.protocol.binding", "HTTPS",
                "edc.dataspace.profiles.dsp2025_1.protocol.namespace", "https://w3id.org/dspace/v1",
                "edc.dataspace.profiles.dsp2025_1.jsonld.context.urls", " https://w3id.org/dspace/v1/context.jsonld ,, "
        )));

        var extension = factory.constructInstance(DataspaceProfileConfigurationExtension.class);
        extension.initialize(context);

        var captor = ArgumentCaptor.forClass(DataspaceProfileContext.class);
        verify(registry).register(captor.capture());
        assertThat(captor.getValue().jsonLdContextsUrl())
                .containsExactly("https://w3id.org/dspace/v1/context.jsonld");
    }
}
