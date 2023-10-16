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

package org.eclipse.edc.iam.identitytrust.transform;

public interface TestData {
    String TEST_NAMESPACE = "https://here.is.the/thing/";
    String EXAMPLE_VC_JSONLD = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v2"
              ],
              "id": "http://university.example/credentials/3732",
              "type": ["VerifiableCredential", "ExampleDegreeCredential"],
              "issuer": {
                "id": "https://university.example/issuers/565049",
                "name": "Example University",
                "description": "A public university focusing on teaching examples."
              },
              "validFrom": "2015-05-10T12:30:00Z",
              "validUntil":"2023-05-12T23:00:00Z",
              "name": "Example University Degree",
              "description": "2015 Bachelor of Science and Arts Degree",
              "credentialSubject": {
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "degree": {
                  "degreetype": "ExampleBachelorDegree",
                  "subtype": "Bachelor of Science and Arts"
                }
              },
              "credentialStatus": {
                  "id": "https://university.example/credentials/status/3#94567",
                  "type": "StatusList2021Entry",
                  "statusPurpose": "revocation",
                  "statusListIndex": "94567",
                  "statusListCredential": "https://university.example/credentials/status/3"
              }
            }
            """;
    String EXAMPLE_VC_JSONLD_ISSUER_IS_URL = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v2"
              ],
              "id": "http://university.example/credentials/3732",
              "type": ["VerifiableCredential", "ExampleDegreeCredential"],
              "issuer": "https://university.example/issuers/565049",
              "validFrom": "2015-05-10T12:30:00Z",
              "validUntil":"2023-05-12T23:00:00Z",
              "name": "Example University Degree",
              "description": "2015 Bachelor of Science and Arts Degree",
              "credentialSubject": {
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "degree": {
                  "degreetype": "ExampleBachelorDegree",
                  "subtype": "Bachelor of Science and Arts"
                }
              },
              "credentialStatus": {
                  "id": "https://university.example/credentials/status/3#94567",
                  "type": "StatusList2021Entry",
                  "statusPurpose": "revocation",
                  "statusListIndex": "94567",
                  "statusListCredential": "https://university.example/credentials/status/3"
              }
            }
            """;
    String EXAMPLE_VP_JSONLD = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v2"
              ],
              "id": "test-id",
              "type": "VerifiablePresentation",
              "verifiableCredential": [
                 {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v2"
                  ],
                  "id": "http://university.example/credentials/3732",
                  "type": ["VerifiableCredential", "ExampleDegreeCredential"],
                  "issuer": {
                    "id": "https://university.example/issuers/565049",
                    "name": "Example University",
                    "description": "A public university focusing on teaching examples."
                  },
                  "validFrom": "2015-05-10T12:30:00Z",
                  "validUntil":"2023-05-12T23:00:00Z",
                  "name": "Example University Degree",
                  "description": "2015 Bachelor of Science and Arts Degree",
                  "credentialSubject": {
                    "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                    "degree": {
                      "degreetype": "ExampleBachelorDegree",
                      "subtype": "Bachelor of Science and Arts"
                    }
                  },
                  "credentialStatus": {
                      "id": "https://university.example/credentials/status/3#94567",
                      "type": "StatusList2021Entry",
                      "statusPurpose": "revocation",
                      "statusListIndex": "94567",
                      "statusListCredential": "https://university.example/credentials/status/3"
                  }
                }
                ],
              "holder":"did:web:test-holder",
              "proof": {
                "type": "AnonCredPresentationProofv1",
                "proofValue": "DgYdYMUYHURJLD7xdnWRinqWCEY5u5fK...j915Lt3hMzLHoPiPQ9sSVfRrs1D"
              }
            }
            """;
}
