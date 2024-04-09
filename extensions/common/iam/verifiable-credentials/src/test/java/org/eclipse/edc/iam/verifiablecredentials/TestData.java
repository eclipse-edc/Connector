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
 *
 */

package org.eclipse.edc.iam.verifiablecredentials;

public class TestData {
    // test data taken from https://www.w3.org/TR/2023/WD-vc-status-list-20230427/#example-example-statuslist2021credential-0
    public static final String STATUS_LIST_CREDENTIAL_SUBJECT_IS_ARRAY = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://w3id.org/vc/status-list/2021/v1"
              ],
              "id": "https://example.com/credentials/status/3",
              "type": ["VerifiableCredential", "StatusList2021Credential"],
              "issuer": "did:example:12345",
              "issued": "2021-04-05T14:27:40Z",
              "credentialSubject": [
              {
                "id": "https://example.com/status/3#list",
                "type": "StatusList2021",
                "https://w3id.org/vc/status-list#statusPurpose": "revocation",
                "https://w3id.org/vc/status-list#encodedList": "H4sIAAAAAAAAA-3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA"
              }
              ]
            }
            """;

    public static final String STATUS_LIST_CREDENTIAL_SINGLE_SUBJECT = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://w3id.org/vc/status-list/2021/v1"
              ],
              "id": "https://example.com/credentials/status/3",
              "type": ["VerifiableCredential", "StatusList2021Credential"],
              "issuer": "did:example:12345",
              "issued": "2021-04-05T14:27:40Z",
              "credentialSubject": {
                "id": "https://example.com/status/3#list",
                "type": "StatusList2021",
                "https://w3id.org/vc/status-list#statusPurpose": "revocation",
                "https://w3id.org/vc/status-list#encodedList": "H4sIAAAAAAAAA-3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA"
              }
            }
            """;
}
