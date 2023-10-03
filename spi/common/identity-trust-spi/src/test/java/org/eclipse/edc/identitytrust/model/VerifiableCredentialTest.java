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

package org.eclipse.edc.identitytrust.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerifiableCredentialTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void buildMinimalVc() {
        assertThatNoException().isThrownBy(() -> VerifiableCredential.Builder.newInstance()
                .credentialSubject(new CredentialSubject())
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(Date.from(now()))
                .proof(TestFunctions.createProof())
                .build());
    }

    @Test
    void assertDefaultValues() {
        var vc = VerifiableCredential.Builder.newInstance()
                .credentialSubject(new CredentialSubject())
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(Date.from(now()))
                .proof(TestFunctions.createProof())
                .build();
        assertThat(vc.getContexts()).containsExactly(VerifiableCredential.DEFAULT_CONTEXT);
        assertThat(vc.getTypes()).containsExactly(VerifiableCredential.DEFAULT_TYPE);
    }

    @Test
    void build_emptyContexts() {
        assertThatThrownBy(() -> VerifiableCredential.Builder.newInstance()
                .credentialSubject(new CredentialSubject())
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(Date.from(now()))
                .proof(TestFunctions.createProof())
                .contexts(new ArrayList<>())
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void build_emptyTypes() {
        assertThatThrownBy(() -> VerifiableCredential.Builder.newInstance()
                .credentialSubject(new CredentialSubject())
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(Date.from(now()))
                .proof(TestFunctions.createProof())
                .types(new ArrayList<>())
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void build_emptyProofs() {
        assertThatThrownBy(() -> VerifiableCredential.Builder.newInstance()
                .credentialSubject(new CredentialSubject())
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(Date.from(now()))
                .proofs(new ArrayList<>())
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

}