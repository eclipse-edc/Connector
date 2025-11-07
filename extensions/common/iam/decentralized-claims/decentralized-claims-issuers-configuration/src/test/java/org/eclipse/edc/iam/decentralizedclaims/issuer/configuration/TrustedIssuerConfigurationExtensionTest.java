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

package org.eclipse.edc.iam.decentralizedclaims.issuer.configuration;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class TrustedIssuerConfigurationExtensionTest {

    private final TrustedIssuerRegistry trustedIssuerRegistry = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(TrustedIssuerRegistry.class, trustedIssuerRegistry);
        context.registerService(TypeManager.class, new JacksonTypeManager());
    }

    @Test
    void initialize_issuerWithSupportedTypes(ServiceExtensionContext context, TrustedIssuerConfigurationExtension ext) {
        var cfg = ConfigFactory.fromMap(Map.of("issuer1.id", "issuer1", "issuer1.supportedtypes", "[\"type1\", \"type2\"]"));
        when(context.getConfig("edc.iam.trusted-issuer")).thenReturn(cfg);

        ext.initialize(context);

        verify(trustedIssuerRegistry).register(argThat(issuer -> issuer.id().equals("issuer1")), eq("type1"));
        verify(trustedIssuerRegistry).register(argThat(issuer -> issuer.id().equals("issuer1")), eq("type1"));
    }

    @Test
    void initialize_issuerWithoutSupportedType(ServiceExtensionContext context, TrustedIssuerConfigurationExtension ext) {
        var cfg = ConfigFactory.fromMap(Map.of("issuer1.id", "issuer1"));
        when(context.getConfig("edc.iam.trusted-issuer")).thenReturn(cfg);

        ext.initialize(context);

        verify(trustedIssuerRegistry).register(argThat(issuer -> issuer.id().equals("issuer1")), eq(TrustedIssuerRegistry.WILDCARD));
    }

    @Test
    void initialize_WithNoIssuer(ServiceExtensionContext context, TrustedIssuerConfigurationExtension ext, Monitor monitor) {
        var cfg = ConfigFactory.fromMap(Map.of());
        when(context.getConfig("edc.iam.trusted-issuer")).thenReturn(cfg);

        ext.initialize(context);

        verifyNoMoreInteractions(trustedIssuerRegistry);

        verify(monitor).warning("The list of trusted issuers is empty");

    }

    @Test
    void initialize_withProperties(ServiceExtensionContext context, TrustedIssuerConfigurationExtension ext) {
        var properties = "{\"custom\": \"test\"}";
        var cfg = ConfigFactory.fromMap(Map.of("issuer1.id", "issuerId1", "issuer1.properties", properties));
        when(context.getConfig("edc.iam.trusted-issuer")).thenReturn(cfg);

        ext.initialize(context);

        verify(trustedIssuerRegistry).register(argThat(issuer -> issuer.additionalProperties().get("custom").equals("test")), eq(TrustedIssuerRegistry.WILDCARD));
    }

    @Test
    void initialize_withTwoIssuers(ServiceExtensionContext context, TrustedIssuerConfigurationExtension ext) {
        var cfg = ConfigFactory.fromMap(Map.of("issuer1.id", "issuerId1", "issuer2.id", "issuerId2"));
        when(context.getConfig("edc.iam.trusted-issuer")).thenReturn(cfg);

        ext.initialize(context);

        var issuers = ArgumentCaptor.forClass(Issuer.class);

        verify(trustedIssuerRegistry, times(2)).register(issuers.capture(), any());

        assertThat(issuers.getAllValues()).hasSize(2)
                .extracting(Issuer::id)
                .contains("issuerId1", "issuerId2");
    }
}
