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

import static java.time.Instant.now;
import static org.eclipse.edc.identitytrust.model.CredentialFormat.JSON_LD;

public class TestFunctions {

    static VerifiableCredential createCredential() {
        return VerifiableCredential.Builder.newInstance("rest-vc", JSON_LD)
                .credentialSubject(new CredentialSubject())
                .type("test-type")
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(now())
                .build();
    }
}
