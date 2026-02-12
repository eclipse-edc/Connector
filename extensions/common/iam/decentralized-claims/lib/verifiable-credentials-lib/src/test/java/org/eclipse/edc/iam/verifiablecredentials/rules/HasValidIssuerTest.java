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

package org.eclipse.edc.iam.verifiablecredentials.rules;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createCredentialBuilder;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HasValidIssuerTest {

    private static final String ISSUER = "did:web:issuer1";

    private final TrustedIssuerRegistry trustedIssuerRegistry = mock();
    private final HasValidIssuer rule = new HasValidIssuer(trustedIssuerRegistry);

    @DisplayName("Issuer is trusted for any credential type")
    @Test
    void isTrustedForAnyType_shouldReturnSuccess() {
        when(trustedIssuerRegistry.getSupportedTypes(new Issuer(ISSUER, Map.of()))).thenReturn(Set.of(TrustedIssuerRegistry.WILDCARD));

        var vc = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of("type1", "type2"))
                .build();

        assertThat(rule.apply(vc)).isSucceeded();
    }

    @DisplayName("Issuer is trusted for the provided credential type")
    @Test
    void isTrustedForType_shouldReturnSuccess() {
        when(trustedIssuerRegistry.getSupportedTypes(new Issuer(ISSUER, Map.of()))).thenReturn(Set.of("type3", "type1"));

        var vc = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of("type1", "type2"))
                .build();

        assertThat(rule.apply(vc)).isSucceeded();
    }

    @DisplayName("Issuer is not trusted for the provided credential type")
    @Test
    void isNotTrustedForType_shouldFail() {
        when(trustedIssuerRegistry.getSupportedTypes(new Issuer(ISSUER, Map.of()))).thenReturn(Set.of("type2", "type3"));

        var vc = createCredentialBuilder()
                .issuer(new Issuer(ISSUER, Map.of()))
                .types(List.of("type1"))
                .build();

        assertThat(rule.apply(vc))
                .isFailed()
                .detail()
                .isEqualTo("Credential types '[type1]' are not supported for issuer '%s'".formatted(ISSUER));
    }

}