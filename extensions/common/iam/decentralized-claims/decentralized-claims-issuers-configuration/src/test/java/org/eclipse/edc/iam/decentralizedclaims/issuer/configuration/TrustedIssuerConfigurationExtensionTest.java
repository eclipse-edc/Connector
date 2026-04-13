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

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

@ExtendWith(DependencyInjectionExtension.class)
public class TrustedIssuerConfigurationExtensionTest {

    private final TrustedIssuerRegistry trustedIssuerRegistry = mock();

    @BeforeEach
    void setup(TestExtensionContext context) {
        context.registerService(TrustedIssuerRegistry.class, trustedIssuerRegistry);
        context.registerService(TypeManager.class, new JacksonTypeManager());
    }

    @ValueSource(strings = {"edc.iam.trusted-issuer", "edc.iam.trustedissuer"})
    @ParameterizedTest
    void initialize_issuerWithSupportedTypes(String prefix, TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                prefix + ".issuer1.id", "issuer1",
                prefix + ".issuer1.supportedtypes", "[\"type1\", \"type2\"]")));

        var ext = factory.constructInstance(TrustedIssuerConfigurationExtension.class);
        ext.initialize(context);

        verify(trustedIssuerRegistry).register(argThat(issuer -> issuer.id().equals("issuer1")), eq("type1"));
        verify(trustedIssuerRegistry).register(argThat(issuer -> issuer.id().equals("issuer1")), eq("type1"));
    }

    @ValueSource(strings = {"edc.iam.trusted-issuer", "edc.iam.trustedissuer"})
    @ParameterizedTest
    void initialize_issuerWithoutSupportedType(String prefix, TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(prefix + ".issuer1.id", "issuer1")));

        var ext = factory.constructInstance(TrustedIssuerConfigurationExtension.class);
        ext.initialize(context);

        verify(trustedIssuerRegistry).register(argThat(issuer -> issuer.id().equals("issuer1")), eq(TrustedIssuerRegistry.WILDCARD));
    }

    @Test
    void initialize_WithNoIssuer(TestExtensionContext context, ObjectFactory factory, Monitor monitor) {
        context.setConfig(ConfigFactory.empty());

        var ext = factory.constructInstance(TrustedIssuerConfigurationExtension.class);
        ext.initialize(context);

        verifyNoMoreInteractions(trustedIssuerRegistry);
        verify(monitor).warning("No configured trusted issuer under 'edc.iam.trustedissuer' setting group.");
    }

    @ValueSource(strings = {"edc.iam.trusted-issuer", "edc.iam.trustedissuer"})
    @ParameterizedTest
    void initialize_withProperties(String prefix, TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                prefix + ".issuer1.id", "issuerId1",
                prefix + ".issuer1.properties", "{\"custom\": \"test\"}")));

        var ext = factory.constructInstance(TrustedIssuerConfigurationExtension.class);
        ext.initialize(context);

        verify(trustedIssuerRegistry).register(argThat(issuer -> issuer.additionalProperties().get("custom").equals("test")), eq(TrustedIssuerRegistry.WILDCARD));
    }

    @ValueSource(strings = {"edc.iam.trusted-issuer", "edc.iam.trustedissuer"})
    @ParameterizedTest
    void initialize_withTwoIssuers(String prefix, TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                prefix + ".issuer1.id", "issuerId1",
                prefix + ".issuer2.id", "issuerId2")));

        var ext = factory.constructInstance(TrustedIssuerConfigurationExtension.class);
        ext.initialize(context);

        var issuers = ArgumentCaptor.forClass(Issuer.class);

        verify(trustedIssuerRegistry, times(2)).register(issuers.capture(), any());

        assertThat(issuers.getAllValues()).hasSize(2)
                .extracting(Issuer::id)
                .contains("issuerId1", "issuerId2");
    }
}
