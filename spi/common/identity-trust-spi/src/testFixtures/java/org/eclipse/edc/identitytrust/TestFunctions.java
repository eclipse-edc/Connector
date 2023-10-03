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

package org.eclipse.edc.identitytrust;

import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.model.VerifiablePresentation;

import java.net.URI;

import static java.time.Instant.now;
import static org.eclipse.edc.identitytrust.model.CredentialFormat.JSON_LD;

public class TestFunctions {

    public static VerifiableCredential createCredential() {
        return createCredentialBuilder().build();
    }

    public static VerifiableCredential.Builder createCredentialBuilder() {
        return VerifiableCredential.Builder.newInstance("test-vc", JSON_LD)
                .credentialSubject(new CredentialSubject())
                .type("test-type")
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(now());
    }

    public static VerifiableCredential.Builder createCredentialBuilder(String rawVc, CredentialFormat format) {
        return VerifiableCredential.Builder.newInstance(rawVc, format)
                .credentialSubject(new CredentialSubject())
                .type("test-type")
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(now());
    }

    public static VerifiablePresentation.Builder createPresentationBuilder(String rawVp, CredentialFormat format) {
        return VerifiablePresentation.Builder.newInstance(rawVp, format)
                .credential(createCredentialBuilder().build())
                .holder("did:web:testholder234")
                .id("test-id");
    }
}
