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

package org.eclipse.edc.iam.identitytrust.verification;

public interface TestData {

    // taken straight from here: https://www.w3.org/TR/vc-data-model/#example-verifiable-presentation-using-jwt-compact-serialization-non-normative
    String VC_CONTENT_FOR_JWT_DEGREE_EXAMPLE = """
            {
                "@context": [
                  "https://www.w3.org/2018/credentials/v1",
                  "https://www.w3.org/2018/credentials/examples/v1"
                ],
                "type": [
                  "VerifiableCredential",
                  "UniversityDegreeCredential"
                ],
                "credentialSubject": {
                  "id": "degreeSub"
                  "degree": {
                    "type": "BachelorDegree",
                    "name": "<span lang='fr-CA'>Baccalauréat en musiques numériques</span>"
                  }
                }
              }
            """;

    String VC_CONTENT_FOR_JWT_CERTIFICATE_EXAMPLE = """
            {
                "@context": [
                  "https://www.w3.org/2018/credentials/v1",
                  "https://www.w3.org/2018/credentials/examples/v1"
                ],
                "type": [
                  "VerifiableCredential",
                  "IsoCertificateCredential"
                ],
                "credentialSubject": {
                  "id": "isoCred"
                  "degree": {
                    "type": "Iso9001Certificate",
                    "name": "ISO 9001 Certificate for excellence in many things"
                  }
                }
              }
            """;

    // taken straight from here: https://www.w3.org/TR/vc-data-model/#example-verifiable-presentation-using-jwt-compact-serialization-non-normative
    String VP_CONTENT_FOR_JWT = """
            {
                "@context": [
                  "https://www.w3.org/2018/credentials/v1",
                  "https://www.w3.org/2018/credentials/examples/v1"
                ],
                "type": [
                  "VerifiablePresentation",
                  "CredentialManagerPresentation"
                ],
                "verifiableCredential": [
                  %s
                ]
            }
            """;
}
