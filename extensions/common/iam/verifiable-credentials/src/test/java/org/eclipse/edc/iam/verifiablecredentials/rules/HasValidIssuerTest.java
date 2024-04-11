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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createCredentialBuilder;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class HasValidIssuerTest {

    @DisplayName("Issuer (string) is in the list of valid issuers")
    @Test
    void hasValidIssuer_string() {
        var vc = createCredentialBuilder()
                .issuer(new Issuer("did:web:issuer2", Map.of()))
                .build();
        assertThat(new HasValidIssuer(List.of("did:web:issuer1", "did:web:issuer2")).apply(vc)).isSucceeded();
    }

    @DisplayName("Issuer (object) is in the list of valid issuers")
    @Test
    void hasValidIssuer_object() {
        var vc = createCredentialBuilder()
                .issuer(new Issuer("did:web:issuer1", Map.of("name", "test issuer company")))
                .build();
        assertThat(new HasValidIssuer(List.of("did:web:issuer1", "did:web:issuer2")).apply(vc)).isSucceeded();
    }

    @DisplayName("Issuer (string) is not in the list of valid issuers")
    @Test
    void invalidIssuer_string() {
        var vc = createCredentialBuilder()
                .issuer(new Issuer("did:web:invalid", Map.of()))
                .build();
        assertThat(new HasValidIssuer(List.of("did:web:issuer1", "did:web:issuer2")).apply(vc)).isFailed()
                .detail().isEqualTo("Issuer 'did:web:invalid' is not in the list of trusted issuers");
    }

    @DisplayName("Issuer (object) is not in the list of valid issuers")
    @Test
    void invalidIssuer_object() {
        var vc = createCredentialBuilder()
                .issuer(new Issuer("did:web:invalid", Map.of("id", "did:web:invalid", "name", "test issuer company")))
                .build();
        assertThat(new HasValidIssuer(List.of("did:web:issuer1", "did:web:issuer2")).apply(vc)).isFailed()
                .detail().isEqualTo("Issuer 'did:web:invalid' is not in the list of trusted issuers");
    }
}