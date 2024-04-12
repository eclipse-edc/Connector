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

package org.eclipse.edc.iam.verifiablecredentials.spi.model;

import org.eclipse.edc.json.JacksonTypeManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerifiableCredentialTest {

    @Test
    void serDeser() {
        var typeManager = new JacksonTypeManager();
        var vc = VerifiableCredential.Builder.newInstance()
                .credentialSubject(new CredentialSubject())
                .issuer(new Issuer("http://test.issuer", Map.of()))
                .issuanceDate(now())
                .type("test-type")
                .build();

        var serialized = typeManager.writeValueAsString(vc);
        assertThat(serialized).contains("\"type\"");
        assertThat(serialized).contains("\"credentialSubject\"");

        var deserialized = typeManager.readValue(serialized, VerifiableCredential.class);

        assertThat(deserialized).usingRecursiveComparison().isEqualTo(vc);
    }

    @Test
    void buildMinimalVc() {
        assertThatNoException().isThrownBy(() -> VerifiableCredential.Builder.newInstance()
                .credentialSubject(new CredentialSubject())
                .issuer(new Issuer("http://test.issuer", Map.of()))
                .issuanceDate(now())
                .type("test-type")
                .build());
    }

    @Test
    void build_emptyContexts() {
        assertThatThrownBy(() -> VerifiableCredential.Builder.newInstance()
                .credentialSubject(new CredentialSubject())
                .issuer(new Issuer("http://test.issuer", Map.of()))
                .issuanceDate(now())
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void build_emptyTypes() {
        assertThatThrownBy(() -> VerifiableCredential.Builder.newInstance()
                .credentialSubject(new CredentialSubject())
                .issuer(new Issuer("http://test.issuer", Map.of()))
                .issuanceDate(now())
                .types(new ArrayList<>())
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void build_emptyProofs() {
        assertThatThrownBy(() -> VerifiableCredential.Builder.newInstance()
                .credentialSubject(new CredentialSubject())
                .issuer(new Issuer("http://test.issuer", Map.of()))
                .issuanceDate(now())
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

}
