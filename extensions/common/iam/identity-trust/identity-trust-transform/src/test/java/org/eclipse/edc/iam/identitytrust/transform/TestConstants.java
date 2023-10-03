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

public interface TestConstants {
    String TEST_NAMESPACE = "https://here.is.the/thing/";
    String EXAMPLE_VC_JSONLD = """
            {
              "@context": [
                "https://www.w3.org/ns/credentials/v2",
                "https://www.w3.org/ns/credentials/examples/v2"
              ],
              "id": "http://university.example/credentials/3732",
              "type": ["VerifiableCredential", "ExampleDegreeCredential"],
              "issuer": {
                "id": "https://university.example/issuers/565049",
                "name": "Example University",
                "description": "A public university focusing on teaching examples."
              },
              "validFrom": "2015-05-10T12:30:00Z",
              "name": "Example University Degree",
              "description": "2015 Bachelor of Science and Arts Degree",
              "credentialSubject": {
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "degree": {
                  "type": "ExampleBachelorDegree",
                  "subtype": "Bachelor of Science and Arts"
                }
              }
            }
            """;
    String EXAMPLE_VP_JSONLD = """
            {
              "id": "test-id",
              "type": "VerifiablePresentation",
              "holder": "did:web:test-holder",
              "verifiableCredential": [{
                "issuanceDate": "2023-06-12T13:13:30Z",
                "credentialSubject": {
                  "id": "did:web:localhost:test-subject-id",
                  "type": "https://org.eclipse.edc/SomeFancyCredential"
                },
                "id": "https://org.eclipse.edc/testcases/t0001",
                "type": [
                  "VerifiableCredential", "SomeFancyCredential"
                ],
                "issuer": "did:web:localhost:test-issuer",
                "expirationDate": "2024-12-31T23:00:00Z",
                "sec:proof": {
                  "type": "JsonWebSignature2020",
                  "created": "2022-12-31T23:00:00Z",
                  "proofPurpose": "assertionMethod",
                  "verificationMethod": "https://org.eclipse.edc/verification-method",
                  "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFUzM4NCJ9..dbi6LFkdeBeCz3sHaxRRFVJC2_rF8Z_oYqaoNOpYtzQh61WP78pK7nKT53WsE-7uiBUMamLA8vEGJpFQ3h4MXDi2OKh1YDpphS_pwyDkqYbsguMs2KYqPxe8t1OC2G1o"
                }
              }],
              "proof": {
                "type": "JsonWebSignature2020",
                "created": "2022-12-31T23:00:00Z",
                "proofPurpose": "assertionMethod",
                "verificationMethod": "https://org.eclipse.edc/verification-method",
                "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFUzM4NCJ9..qVCNVL_jxQdqa509KPTjRERopJiRtW1CqctVD_uGtUlCNF9oM2eB1L821YvjW0VjZjP6XdS5bLfQpG3azg9Hm8-L4vFBiH8HgEdVllHVcmO1odG-2GQAnhdP6Kdg42Wh"
              },
              "@context": [
                "https://www.w3.org/ns/did/v1",
                "https://www.w3.org/2018/credentials/v1",
                "https://w3id.org/security/suites/jws-2020/v1",
                "https://www.w3.org/ns/did/v1"
              ]
            }
            """;
}
