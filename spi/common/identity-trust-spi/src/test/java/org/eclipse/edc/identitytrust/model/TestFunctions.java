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

import java.net.URI;
import java.util.Date;

import static java.time.Instant.now;

public class TestFunctions {
    static Proof createProof() {
        return Proof.Builder.newInstance()
                .type("test-type")
                .created(Date.from(now()))
                .verificationMethod(URI.create("http://verification.method/"))
                .proofContent("foo", "bar")
                .proofPurpose("assertionMethod")
                .build();
    }

    static VerifiableCredential createCredential() {
        return VerifiableCredential.Builder.newInstance()
                .credentialSubject(new CredentialSubject())
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(Date.from(now()))
                .proof(createProof())
                .build();
    }
}
