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
 *       Cofinity-X - updates for VCDM 2.0
 *
 */

package org.eclipse.edc.iam.verifiablecredentials.spi;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentation;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;

import java.util.Map;

import static java.time.Instant.now;

public class TestFunctions {

    public static final Issuer TRUSTED_ISSUER = new Issuer("http://test.issuer", Map.of());

    public static VerifiableCredential createCredential() {
        return createCredentialBuilder().build();
    }

    public static VerifiableCredential.Builder createCredentialBuilder() {
        return VerifiableCredential.Builder.newInstance()
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("test-subject-id")
                        .claim("test-claim", "test-value")
                        .build())
                .type("test-type")
                .issuer(TRUSTED_ISSUER)
                .issuanceDate(now());
    }

    public static VerifiablePresentation.Builder createPresentationBuilder() {
        return VerifiablePresentation.Builder.newInstance()
                .credential(createCredentialBuilder().build())
                .holder("did:web:testholder234")
                .id("test-id");
    }

    public static VerifiablePresentationContainer createPresentationContainer() {
        return new VerifiablePresentationContainer("RAW_VP", CredentialFormat.VC1_0_LD, createPresentationBuilder().type("VerifiableCredential").build());
    }
}
