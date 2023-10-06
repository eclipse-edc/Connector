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

import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.model.VerifiablePresentation;

import java.net.URI;

import static java.time.Instant.now;

public class TestFunctions {

    public static VerifiableCredential createCredential() {
        return createCredentialBuilder().build();
    }

    public static VerifiableCredential.Builder createCredentialBuilder() {
        return VerifiableCredential.Builder.newInstance()
                .credentialSubject(new CredentialSubject())
                .type("test-type")
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(now());
    }

    public static VerifiablePresentation.Builder createPresentationBuilder() {
        return VerifiablePresentation.Builder.newInstance()
                .credential(createCredentialBuilder().build())
                .holder("did:web:testholder234")
                .id("test-id");
    }
}
