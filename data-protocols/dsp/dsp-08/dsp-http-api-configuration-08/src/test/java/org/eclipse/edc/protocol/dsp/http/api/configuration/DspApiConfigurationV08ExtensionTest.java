/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *       Cofinity-X - extract webhook and port mapping creation
 *
 */

package org.eclipse.edc.protocol.dsp.http.api.configuration;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE_V_08;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DspApiConfigurationV08ExtensionTest {
    
    private final String webhookUrl = "http://webhook";
    
    private final TypeManager typeManager = mock();
    private final JsonLd jsonLd = mock();
    private final DataspaceProfileContextRegistry dataspaceProfileContextRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TypeManager.class, typeManager);
        context.registerService(JsonLd.class, jsonLd);
        context.registerService(DspBaseWebhookAddress.class, () -> webhookUrl);
        context.registerService(DataspaceProfileContextRegistry.class, dataspaceProfileContextRegistry);
        TypeTransformerRegistry typeTransformerRegistry = mock();
        when(typeTransformerRegistry.forContext(any())).thenReturn(mock());
        context.registerService(TypeTransformerRegistry.class, typeTransformerRegistry);

        when(typeManager.getMapper(any())).thenReturn(mock());
    }

    @Test
    void shouldUseInjectedBaseWebhook(DspApiConfigurationV08Extension extension, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.empty());

        extension.initialize(context);

        verify(dataspaceProfileContextRegistry).registerDefault(argThat(it -> it.name().equals(DATASPACE_PROTOCOL_HTTP) && it.webhook().url().equals(webhookUrl)));
    }

    @Test
    void shouldRegisterCorrectProtocolWebhooks_whenWellKnownPathsEnabled(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.dsp.well-known-path.enabled", "true"))
        );
        var extension = factory.constructInstance(DspApiConfigurationV08Extension.class);

        extension.initialize(context);

        verify(dataspaceProfileContextRegistry).registerDefault(argThat(it -> it.name().equals(DATASPACE_PROTOCOL_HTTP) && it.webhook().url().equals(webhookUrl)));
    }

    @Test
    void initialize_shouldRegisterNamespaces(DspApiConfigurationV08Extension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(jsonLd).registerNamespace(DCAT_PREFIX, DCAT_SCHEMA, DSP_SCOPE_V_08);
        verify(jsonLd).registerNamespace(DCT_PREFIX, DCT_SCHEMA, DSP_SCOPE_V_08);
        verify(jsonLd).registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, DSP_SCOPE_V_08);
        verify(jsonLd).registerNamespace(VOCAB, EDC_NAMESPACE, DSP_SCOPE_V_08);
        verify(jsonLd).registerNamespace(EDC_PREFIX, EDC_NAMESPACE, DSP_SCOPE_V_08);
        verify(jsonLd).registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, DSP_SCOPE_V_08);
    }

}
