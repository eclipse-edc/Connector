/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.api.configuration;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
import org.eclipse.edc.spi.protocol.ProtocolWebhookRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2024_1_PATH;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DspApiConfigurationV2024ExtensionTest {
    
    private final String webhookUrl = "http://webhook";
    
    private final TypeManager typeManager = mock();
    private final JsonLd jsonLd = mock();
    private final ProtocolWebhookRegistry protocolWebhookRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TypeManager.class, typeManager);
        context.registerService(JsonLd.class, jsonLd);
        context.registerService(DspBaseWebhookAddress.class, () -> webhookUrl);
        context.registerService(ProtocolWebhookRegistry.class, protocolWebhookRegistry);
        TypeTransformerRegistry typeTransformerRegistry = mock();
        when(typeTransformerRegistry.forContext(any())).thenReturn(mock());
        context.registerService(TypeTransformerRegistry.class, typeTransformerRegistry);

        when(typeManager.getMapper(any())).thenReturn(mock());
    }

    @Test
    void shouldUseInjectedBaseWebhook(DspApiConfigurationV2024Extension extension, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.empty());

        extension.initialize(context);

        verify(protocolWebhookRegistry).registerWebhook(eq(DATASPACE_PROTOCOL_HTTP), argThat(webhook -> webhook.url().equals(webhookUrl)));
        verify(protocolWebhookRegistry).registerWebhook(eq(DATASPACE_PROTOCOL_HTTP_V_2024_1), argThat(webhook -> webhook.url().equals(webhookUrl + V_2024_1_PATH)));
    }

    @Test
    void shouldRegisterCorrectProtocolWebhooks_whenWellKnownPathsEnabled(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.dsp.well-known-path.enabled", "true"))
        );
        var extension = factory.constructInstance(DspApiConfigurationV2024Extension.class);

        extension.initialize(context);

        verify(protocolWebhookRegistry).registerWebhook(eq(DATASPACE_PROTOCOL_HTTP), argThat(webhook -> webhook.url().equals(webhookUrl)));
        verify(protocolWebhookRegistry).registerWebhook(eq(DATASPACE_PROTOCOL_HTTP_V_2024_1), argThat(webhook -> webhook.url().equals(webhookUrl)));
    }

    @ParameterizedTest
    @ValueSource(strings = {DSP_SCOPE_V_08, DSP_SCOPE_V_2024_1})
    void initialize_shouldRegisterNamespaces(String scope, DspApiConfigurationV2024Extension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(jsonLd).registerNamespace(DCAT_PREFIX, DCAT_SCHEMA, scope);
        verify(jsonLd).registerNamespace(DCT_PREFIX, DCT_SCHEMA, scope);
        verify(jsonLd).registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, scope);
        verify(jsonLd).registerNamespace(VOCAB, EDC_NAMESPACE, scope);
        verify(jsonLd).registerNamespace(EDC_PREFIX, EDC_NAMESPACE, scope);
        verify(jsonLd).registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, scope);
    }

}
