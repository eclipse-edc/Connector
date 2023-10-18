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

package org.eclipse.edc.verifiablecredentials.jwt;

public interface TestConstants {
    String CENTRAL_ISSUER_KEY_ID = "#central-issuer-key1";
    String PRESENTER_KEY_ID = "#my-key1";
    String VP_HOLDER_ID = "did:web:test-issuer";
    String MY_OWN_DID = "did:web:myself";
    String CENTRAL_ISSUER_DID = "did:web:some-official-issuer";

    // taken straight from here: https://www.w3.org/TR/vc-data-model/#example-verifiable-presentation-using-jwt-compact-serialization-non-normative
    String VC_CONTENT_DEGREE_EXAMPLE = """
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
                  "id": "degreeSub",
                  "degree": {
                    "type": "BachelorDegree",
                    "name": "<span lang='fr-CA'>Baccalauréat en musiques numériques</span>"
                  }
                }
              }
            """;

    String VC_CONTENT_CERTIFICATE_EXAMPLE = """
            {
                "@context": [
                  "https://www.w3.org/2018/credentials/v1",
                  "https://www.w3.org/2018/credentials/examples/v1"
                ],
                "type": [
                  "VerifiableCredential",
                  "IsoCertificateCredential"
                ],
                "issuer":"did:web:test-issuer",
                "issuanceDate": "2022-10-18T12:56:00Z",
                "credentialSubject": {
                  "id": "https://w3id.org/example/isoCred",
                  "degree": {
                    "type": "Iso9001Certificate",
                    "name": "ISO 9001 Certificate for excellence in many things"
                  }
                }
              }
            """;

    // taken straight from here: https://www.w3.org/TR/vc-data-model/#example-verifiable-presentation-using-jwt-compact-serialization-non-normative
    String VP_CONTENT_TEMPLATE = """
            {
                "@context": [
                  "https://www.w3.org/2018/credentials/v1",
                  "https://www.w3.org/2018/credentials/examples/v1"
                ],
                "id": "https://exapmle.com/test-vp",
                "holder": "https://holder.test.com",
                "type": [
                  "VerifiablePresentation"
                ],
                "verifiableCredential": [
                  %s
                ]
            }
            """;
}
