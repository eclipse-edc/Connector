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
 *       Cofinity-X - updates for VCDM 2.0
 *
 */

package org.eclipse.edc.iam.identitytrust.transform;

import org.jetbrains.annotations.NotNull;

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

    String EXAMPLE_VC_JSONLD_WITH_SCHEMA = """
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
              },
              "credentialSchema": [{
                  "id": "https://example.org/examples/degree.json",
                  "type": "JsonSchema"
                },
                {
                  "id": "https://example.org/examples/alumni.json",
                  "type": "JsonSchema"
              }]
            }
            """;

    String EXAMPLE_VC_SUB_IS_ARRAY_JSONLD = """
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
              "credentialSubject": [{
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "degree": {
                  "degreetype": "ExampleBachelorDegree",
                  "subtype": "Bachelor of Science and Arts"
                }
              },
              {
                "id": "did:example:asdf87ljkfdsa",
                "foo": "bar"
              }
              ],
              "credentialStatus": [
              {
                  "id": "https://university.example/credentials/status/3#94567",
                  "type": "StatusList2021Entry",
                  "statusPurpose": "revocation",
                  "statusListIndex": "94567",
                  "statusListCredential": "https://university.example/credentials/status/3"
              },
              {
                  "id": "https://university.example/credentials/status/5#5234",
                  "type": "StatusList2021Entry",
                  "statusPurpose": "suspension",
                  "statusListIndex": "234789",
                  "statusListCredential": "https://university.example/credentials/status/5"
              }
              ]
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
              },
              "proof": {
                  "type": "Ed25519Signature2020",
                  "created": "2022-02-25T14:58:42Z",
                  "verificationMethod": "https://example.edu/issuers/565049#key-1",
                  "proofPurpose": "assertionMethod",
                  "proofValue": "z3FXQjecWufY46yg5abdVZsXqLhxhueuSoZgNSARiKBk9czhSePTFehP8c3PGfb6a22gkfUKods5D2UAUL5n2Brbx"
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

    String EXAMPLE_JWT_VP = """
            eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImRpZDpleGFtcGxlOjB4YWJjI2
            tleTEifQ.eyJpc3MiOiJkaWQ6ZXhhbXBsZTplYmZlYjFmNzEyZWJjNmYxYzI3NmUxMmVjMjEiLCJqdGkiOiJ1cm46dXVpZDozOTc4MzQ0Zi0
            4NTk2LTRjM2EtYTk3OC04ZmNhYmEzOTAzYzUiLCJhdWQiOiJkaWQ6ZXhhbXBsZTo0YTU3NTQ2OTczNDM2ZjZmNmM0YTRhNTc1NzMiLCJuYmY
            iOjE1NDE0OTM3MjQsImlhdCI6MTU0MTQ5MzcyNCwiZXhwIjoxNTczMDI5NzIzLCJub25jZSI6IjM0M3MkRlNGRGEtIiwidnAiOnsiQGNvbn
            RleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50a
            WFscy9leGFtcGxlcy92MSJdLCJ0eXBlIjpbIlZlcmlmaWFibGVQcmVzZW50YXRpb24iLCJDcmVkZW50aWFsTWFuYWdlclByZXNlbnRhdGlv
            biJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6WyJleUpoYkdjaU9pSlNVekkxTmlJc0luUjVjQ0k2SWtwWFZDSXNJbXRwWkNJNkltUnBaRHB
            sZUdGdGNHeGxPbUZpWm1VeE0yWTNNVEl4TWpBME16RmpNamMyWlRFeVpXTmhZaU5yWlhsekxURWlmUS5leUp6ZFdJaU9pSmthV1E2WlhoaG
            JYQnNaVHBsWW1abFlqRm1OekV5WldKak5tWXhZekkzTm1VeE1tVmpNakVpTENKcWRHa2lPaUpvZEhSd09pOHZaWGhoYlhCc1pTNWxaSFV2W
            TNKbFpHVnVkR2xoYkhNdk16Y3pNaUlzSW1semN5STZJbWgwZEhCek9pOHZaWGhoYlhCc1pTNWpiMjB2YTJWNWN5OW1iMjh1YW5kcklpd2lib
            UptSWpveE5UUXhORGt6TnpJMExDSnBZWFFpT2pFMU5ERTBPVE0zTWpRc0ltVjRjQ0k2TVRVM016QXlPVGN5TXl3aWJtOXVZMlVpT2lJMk5qQ
            WhOak0wTlVaVFpYSWlMQ0oyWXlJNmV5SkFZMjl1ZEdWNGRDSTZXeUpvZEhSd2N6b3ZMM2QzZHk1M015NXZjbWN2TWpBeE9DOWpjbVZrWlc1M
            GFXRnNjeTkyTVNJc0ltaDBkSEJ6T2k4dmQzZDNMbmN6TG05eVp5OHlNREU0TDJOeVpXUmxiblJwWVd4ekwyVjRZVzF3YkdWekwzWXhJbDBzS
            W5SNWNHVWlPbHNpVm1WeWFXWnBZV0pzWlVOeVpXUmxiblJwWVd3aUxDSlZibWwyWlhKemFYUjVSR1ZuY21WbFEzSmxaR1Z1ZEdsaGJDSmRM
            Q0pqY21Wa1pXNTBhV0ZzVTNWaWFtVmpkQ0k2ZXlKa1pXZHlaV1VpT25zaWRIbHdaU0k2SWtKaFkyaGxiRzl5UkdWbmNtVmxJaXdpYm1GdFp
            TSTZJanh6Y0dGdUlHeGhibWM5SjJaeUxVTkJKejVDWVdOallXeGhkWExEcVdGMElHVnVJRzExYzJseGRXVnpJRzUxYmNPcGNtbHhkV1Z6UE
            M5emNHRnVQaUo5ZlgxOS5LTEpvNUdBeUJORDNMRFRuOUg3RlFva0VzVUVpOGpLd1hoR3ZvTjNKdFJhNTF4ck5EZ1hEYjBjcTFVVFlCLXJLNE
            Z0OVlWbVIxTklfWk9GOG9HY183d0FwOFBIYkYySGFXb2RRSW9PQnh4VC00V05xQXhmdDdFVDZsa0gtNFM2VXgzclNHQW1jek1vaEVFZjhlQ
            2VOLWpDOFdla2RQbDZ6S1pRajBZUEIxcng2WDAteGxGQnM3Y2w2V3Q4cmZCUF90WjlZZ1ZXclFtVVd5cFNpb2MwTVV5aXBobXlFYkxaYWdUe
            VBsVXlmbEdsRWRxclpBdjZlU2U2UnR4Snk2TTEtbEQ3YTVIVHphbllUV0JQQVVIRFpHeUdLWGRKdy1XX3gwSVdDaEJ6STh0M2twRzI1M2ZnN
            lYzdFBnSGVLWEU5NGZ6X1FwWWZnLS03a0xzeUJBZlFHYmciXX19.ft_Eq4IniBrr7gtzRfrYj8Vy1aPXuFZU-6_ai0wvaKcsrzI4JkQEKTv
            bJwdvIeuGuTqy7ipO-EYi7V4TvonPuTRdpB7ZHOlYlbZ4wA9WJ6mSVSqDACvYRiFvrOFmie8rgm6GacWatgO4m4NqiFKFko3r58LueFfGw4
            7NK9RcfOkVQeHCq4btaDqksDKeoTrNysF4YS89INa-prWomrLRAhnwLOo1Etp3E4ESAxg73CR2kA5AoMbf5KtFueWnMcSbQkMRdWcGC1VssC
            0tB0JffVjq7ZV6OTyV4kl1-UVgiPLXUTpupFfLRhf9QpqMBjYgP62KvhIvW8BbkGUelYMetA
            """;

    String EXAMPLE_JWT_VP_EMPTY_CREDENTIALS_ARRAY = """
            eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImRpZDpleGFtcGxlOjB4YWJjI2tleTEifQ.eyJpc3MiOiJkaWQ6ZXhhbXBsZTplY
            mZlYjFmNzEyZWJjNmYxYzI3NmUxMmVjMjEiLCJqdGkiOiJ1cm46dXVpZDozOTc4MzQ0Zi04NTk2LTRjM2EtYTk3OC04ZmNhYmEzOTAzYzUiL
            CJhdWQiOiJkaWQ6ZXhhbXBsZTo0YTU3NTQ2OTczNDM2ZjZmNmM0YTRhNTc1NzMiLCJuYmYiOjE1NDE0OTM3MjQsImlhdCI6MTU0MTQ5Mzcy
            NCwiZXhwIjoxNTczMDI5NzIzLCJub25jZSI6IjM0M3MkRlNGRGEtIiwidnAiOnsiQGNvbnRleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzI
            wMTgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy9leGFtcGxlcy92MSJdLCJ0eXBlIjpbIlZl
            cmlmaWFibGVQcmVzZW50YXRpb24iLCJDcmVkZW50aWFsTWFuYWdlclByZXNlbnRhdGlvbiJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6W11
            9fQ.hF9sRPFLaVtYQLh3edAfsbl5oceszAlhPrnzm8eSHd_MHMgKmtUm0iu44lytLpsI6AP38SKJy1BEFfbg9NkKLR6cJe0z7Xqn84C4DaQw
            c4zqaRaMTXhx8QAIH11Zj5sWhoJkCAAQcqhZTXqEVJCQWr1FLJiR4tVytOORZHh1i9AtiMIMqhzleaMSmHgxZXGKnluucFf93H_bKJLRMVIKS
            mi7gJk6Jz8cjQ1Jv1P2UQNagxZ7Y-p6glsE9FxAKXoLBUVsZzITrnrkx9PK91ZeH694dJRa1QmwOIjUnBt9i2-UPs_i-fyebHEkocpV6ETOv5-
            LBtS5puQCOEEkabh07w
            """;

    String EXMPLE_JWT_VP_NO_VP_CLAIM = """
            eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImRpZDpleGFtcGxlOjB4YWJjI2tleTEifQ.eyJpc3MiOiJkaWQ6ZXhhbXBsZTplY
            mZlYjFmNzEyZWJjNmYxYzI3NmUxMmVjMjEiLCJqdGkiOiJ1cm46dXVpZDozOTc4MzQ0Zi04NTk2LTRjM2EtYTk3OC04ZmNhYmEzOTAzYzUiLC
            JhdWQiOiJkaWQ6ZXhhbXBsZTo0YTU3NTQ2OTczNDM2ZjZmNmM0YTRhNTc1NzMiLCJuYmYiOjE1NDE0OTM3MjQsImlhdCI6MTU0MTQ5MzcyNCw
            iZXhwIjoxNTczMDI5NzIzLCJub25jZSI6IjM0M3MkRlNGRGEtIn0.QixkENyxnqRjsQcfkqvP28tRFlNeWDmObVaVZGqVHTQhPAka7NrioDm
            2draiAU7u0OSAtSfBomcLAcRSuNflDT7WQMm-RtzKmxzmdTOjeT0qgB1dwtovmo9cedpCHKYDg0MWrKLLy_EqTlEwgQIdPF43pbTh_QHueM0
            AqguFjo7RYHix77ueBEz9A7yvCFSE0LQRMYzy8Fjc-JA_jaq1G1eg7M-IceCB74Sj52nlbEIbfLxjlgqHzpQ4M4gN-J-9rA5qrReI1uI-hj
            7EbH9P_HKksL5jVPzLNMHLE68Z907J22n3cthZFVPAgpwFgIcIMAVB7uyOVR0P8PA3JdFQg
            """;

    String EXAMPLE_JWT_VP_SINGLE_VC = """
            eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImRpZDpleGFtcGxlOjB4YWJjI2tleTEifQ.eyJpc3MiOiJkaWQ6ZXhhbXBsZTpl
            YmZlYjFmNzEyZWJjNmYxYzI3NmUxMmVjMjEiLCJqdGkiOiJ1cm46dXVpZDozOTc4MzQ0Zi04NTk2LTRjM2EtYTk3OC04ZmNhYmEzOTAzYzU
            iLCJhdWQiOiJkaWQ6ZXhhbXBsZTo0YTU3NTQ2OTczNDM2ZjZmNmM0YTRhNTc1NzMiLCJuYmYiOjE1NDE0OTM3MjQsImlhdCI6MTU0MTQ5Mz
            cyNCwiZXhwIjoxNTczMDI5NzIzLCJub25jZSI6IjM0M3MkRlNGRGEtIiwidnAiOnsiQGNvbnRleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnL
            zIwMTgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy9leGFtcGxlcy92MSJdLCJ0eXBlIjpb
            IlZlcmlmaWFibGVQcmVzZW50YXRpb24iLCJDcmVkZW50aWFsTWFuYWdlclByZXNlbnRhdGlvbiJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI
            6ImV5SmhiR2NpT2lKU1V6STFOaUlzSW5SNWNDSTZJa3BYVkNJc0ltdHBaQ0k2SW1ScFpEcGxlR0Z0Y0d4bE9tRmlabVV4TTJZM01USXhNak
            EwTXpGak1qYzJaVEV5WldOaFlpTnJaWGx6TFRFaWZRLmV5SnpkV0lpT2lKa2FXUTZaWGhoYlhCc1pUcGxZbVpsWWpGbU56RXlaV0pqTm1Ze
            Fl6STNObVV4TW1Wak1qRWlMQ0pxZEdraU9pSm9kSFJ3T2k4dlpYaGhiWEJzWlM1bFpIVXZZM0psWkdWdWRHbGhiSE12TXpjek1pSXNJbWx6
            Y3lJNkltaDBkSEJ6T2k4dlpYaGhiWEJzWlM1amIyMHZhMlY1Y3k5bWIyOHVhbmRySWl3aWJtSm1Jam94TlRReE5Ea3pOekkwTENKcFlYUWl
            PakUxTkRFME9UTTNNalFzSW1WNGNDSTZNVFUzTXpBeU9UY3lNeXdpYm05dVkyVWlPaUkyTmpBaE5qTTBOVVpUWlhJaUxDSjJZeUk2ZXlKQV
            kyOXVkR1Y0ZENJNld5Sm9kSFJ3Y3pvdkwzZDNkeTUzTXk1dmNtY3ZNakF4T0M5amNtVmtaVzUwYVdGc2N5OTJNU0lzSW1oMGRIQnpPaTh2ZD
            NkM0xuY3pMbTl5Wnk4eU1ERTRMMk55WldSbGJuUnBZV3h6TDJWNFlXMXdiR1Z6TDNZeElsMHNJblI1Y0dVaU9sc2lWbVZ5YVdacFlXSnNaVU
            55WldSbGJuUnBZV3dpTENKVmJtbDJaWEp6YVhSNVJHVm5jbVZsUTNKbFpHVnVkR2xoYkNKZExDSmpjbVZrWlc1MGFXRnNVM1ZpYW1WamRDST
            ZleUprWldkeVpXVWlPbnNpZEhsd1pTSTZJa0poWTJobGJHOXlSR1ZuY21WbElpd2libUZ0WlNJNklqeHpjR0Z1SUd4aGJtYzlKMlp5TFVOQk
            p6NUNZV05qWVd4aGRYTERxV0YwSUdWdUlHMTFjMmx4ZFdWeklHNTFiY09wY21seGRXVnpQQzl6Y0dGdVBpSjlmWDE5LktMSm81R0F5Qk5EM0
            xEVG45SDdGUW9rRXNVRWk4akt3WGhHdm9OM0p0UmE1MXhyTkRnWERiMGNxMVVUWUItcks0RnQ5WVZtUjFOSV9aT0Y4b0djXzd3QXA4UEhiRj
            JIYVdvZFFJb09CeHhULTRXTnFBeGZ0N0VUNmxrSC00UzZVeDNyU0dBbWN6TW9oRUVmOGVDZU4takM4V2VrZFBsNnpLWlFqMFlQQjFyeDZYMC
            14bEZCczdjbDZXdDhyZkJQX3RaOVlnVldyUW1VV3lwU2lvYzBNVXlpcGhteUViTFphZ1R5UGxVeWZsR2xFZHFyWkF2NmVTZTZSdHhKeTZNMS
            1sRDdhNUhUemFuWVRXQlBBVUhEWkd5R0tYZEp3LVdfeDBJV0NoQnpJOHQza3BHMjUzZmc2VjN0UGdIZUtYRTk0ZnpfUXBZZmctLTdrTHN5Qk
            FmUUdiZyJ9fQ.31tK3Nd0c8F7cqeS-2wBsQesPMCNNqq1v_9WDRVYksG8RxdVizLSb28KTmfqoxoFNVpVxCOTsit_GWndEhARbZxH3ASgjkn
            3Q46N2216r7psE3a_kUIFC80vetVFDnKMF5B8mlgmiFl6S6fNGAgmJO-VDYw6_pfGvyqzApm7UBTgzALhX6K__uCH9JhQydWrSbwzfcx1E7P
            uyttAjCTBgjIGDLIyVyOtuxcaa4gCFmh2sXt3EvW5J5swVSyCfQu5cJlnA28yHd2jxI2Ry8QmQy65RLFhnDVWBT-zPjxvu0A1lOjy70R8jyl
            7jR40aoI0-hQJ5LFWJjmXPbbiR4MIkg
            """;

    String EXAMPLE_JWT_VP_WITH_LDP_VC = """
            eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImRpZDpleGFtcGxlOjB4YWJjI2tleTEifQ.eyJpc3MiOiJkaWQ6ZXhhbXBsZTplYm
            ZlYjFmNzEyZWJjNmYxYzI3NmUxMmVjMjEiLCJqdGkiOiJ1cm46dXVpZDozOTc4MzQ0Zi04NTk2LTRjM2EtYTk3OC04ZmNhYmEzOTAzYzUiLC
            JhdWQiOiJkaWQ6ZXhhbXBsZTo0YTU3NTQ2OTczNDM2ZjZmNmM0YTRhNTc1NzMiLCJuYmYiOjE1NDE0OTM3MjQsImlhdCI6MTU0MTQ5MzcyNC
            wiZXhwIjoxNTczMDI5NzIzLCJub25jZSI6IjM0M3MkRlNGRGEtIiwidnAiOnsiQGNvbnRleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzIwMTg
            vY3JlZGVudGlhbHMvdjEiLCJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy9leGFtcGxlcy92MSJdLCJ0eXBlIjpbIlZlcml
            maWFibGVQcmVzZW50YXRpb24iLCJDcmVkZW50aWFsTWFuYWdlclByZXNlbnRhdGlvbiJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6W3siQGN
            vbnRleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW5
            0aWFscy9leGFtcGxlcy92MSIsImh0dHBzOi8vdzNpZC5vcmcvc2VjdXJpdHkvc3VpdGVzL2VkMjU1MTktMjAyMC92MSJdLCJpZCI6Imh0dHA
            6Ly9leGFtcGxlLmVkdS9jcmVkZW50aWFscy8zNzMyIiwidHlwZSI6WyJWZXJpZmlhYmxlQ3JlZGVudGlhbCIsIlVuaXZlcnNpdHlEZWdyZWV
            DcmVkZW50aWFsIl0sImlzc3VlciI6Imh0dHBzOi8vZXhhbXBsZS5lZHUvaXNzdWVycy8xNCIsImlzc3VhbmNlRGF0ZSI6IjIwMTAtMDEtMD
            FUMTk6MjM6MjRaIiwiY3JlZGVudGlhbFN1YmplY3QiOnsiaWQiOiJkaWQ6ZXhhbXBsZTplYmZlYjFmNzEyZWJjNmYxYzI3NmUxMmVjMjEiLC
            JkZWdyZWUiOnsidHlwZSI6IkJhY2hlbG9yRGVncmVlIiwibmFtZSI6IkJhY2hlbG9yIG9mIFNjaWVuY2UgYW5kIEFydHMifX0sImNyZWRlbn
            RpYWxTdGF0dXMiOnsiaWQiOiJodHRwczovL2V4YW1wbGUuZWR1L3N0YXR1cy8yNCIsInR5cGUiOiJDcmVkZW50aWFsU3RhdHVzTGlzdDIwMT
            cifSwicHJvb2YiOnsidHlwZSI6IkVkMjU1MTlTaWduYXR1cmUyMDIwIiwiY3JlYXRlZCI6IjIwMjItMDItMjVUMTQ6NTg6NDNaIiwidmVyaWZ
            pY2F0aW9uTWV0aG9kIjoiaHR0cHM6Ly9leGFtcGxlLmVkdS9pc3N1ZXJzLzE0I2tleS0xIiwicHJvb2ZQdXJwb3NlIjoiYXNzZXJ0aW9uTWV0
            aG9kIiwicHJvb2ZWYWx1ZSI6InozQlhzRmZ4MXFKNU5zVGtLcVJFalEzQUdoNlJBbUN3dmd1MUhjRFN6SzNQNVFFZzJUQXc4dWZrdEpCdzhRa
            0FRUmNpTUd5QmY1VDJBSHlSZzJ3MTNVdmhwIn19XX19.GcAjYFJm6KmqZjiYUocN8vEB_UDtKJOl29thxJrWYQeA5HcSAYip_fMvqxbqY7SSN
            2gdTKkZmZhK0SYTrl-zIXIUlB011PYkIWM4WIlr956BQPAHdYA-gosr8KfFX6Jr1-k0c6xYNt-1sWhtrsXepPNqmTI9kIsGL5hpCyMrvnlak7
            rsm3sqVy7PYV_vCzElLqReF7unVTsxhdVIQurLiKfQ66JZTrIty-CZ5F-VulKe5Qzbxgz1-YQa1QDPn9uYWfA4_MxP7ukg6cIky8HgK2iIPBoz
            ZKjRop7QKEM07xL3aP_2gfzXej35qptGtE8y48pWdOkURYFhcMgymCsV7Q
            """;

    String EXAMPLE_JWT_VC = """
            eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2YyI6eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWF
            scy92MSIsImh0dHBzOi8vd3d3LnczLm9yZy8yMDE4L2NyZWRlbnRpYWxzL2V4YW1wbGVzL3YxIl0sImlkIjoiaHR0cDovL2V4YW1wbGUuZWR
            1L2NyZWRlbnRpYWxzLzM3MzIiLCJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiVW5pdmVyc2l0eURlZ3JlZUNyZWRlbnRpYWwiXSw
            iaXNzdWVyIjoiaHR0cHM6Ly9leGFtcGxlLmVkdS9pc3N1ZXJzLzE0IiwiaXNzdWFuY2VEYXRlIjoiMjAxMC0wMS0wMVQxOToyMzoyNFoiLCJ
            jcmVkZW50aWFsU3ViamVjdCI6eyJpZCI6ImRpZDpleGFtcGxlOmViZmViMWY3MTJlYmM2ZjFjMjc2ZTEyZWMyMSIsImRlZ3JlZSI6eyJ0eXB
            lIjoiQmFjaGVsb3JEZWdyZWUiLCJuYW1lIjoiQmFjaGVsb3Igb2YgU2NpZW5jZSBhbmQgQXJ0cyJ9fSwiY3JlZGVudGlhbFN0YXR1cyI6eyJ
            pZCI6Imh0dHBzOi8vZXhhbXBsZS5lZHUvc3RhdHVzLzI0IiwidHlwZSI6IkNyZWRlbnRpYWxTdGF0dXNMaXN0MjAxNyJ9LCJjcmVkZW50aWF
            sU2NoZW1hIjpbeyJpZCI6Imh0dHBzOi8vZXhhbXBsZS5vcmcvZXhhbXBsZXMvZGVncmVlLmpzb24iLCJ0eXBlIjoiSnNvblNjaGVtYSJ9LHs
            iaWQiOiJodHRwczovL2V4YW1wbGUub3JnL2V4YW1wbGVzL2FsdW1uaS5qc29uIiwidHlwZSI6Ikpzb25TY2hlbWEifV19LCJpc3MiOiJodHR
            wczovL2V4YW1wbGUuZWR1L2lzc3VlcnMvMTQiLCJuYmYiOjEyNjIzNzM4MDQsImp0aSI6Imh0dHA6Ly9leGFtcGxlLmVkdS9jcmVkZW50aWF
            scy8zNzMyIiwic3ViIjoiZGlkOmV4YW1wbGU6ZWJmZWIxZjcxMmViYzZmMWMyNzZlMTJlYzIxIn0.IjVESDTm094UZor3AWJY-wC7a9DBWF_
            fzm4q9M-H6F7F8YVe3YF_gmzKNblR3l8VeaASD4R0YwR1rawVA2mfNQ
            """;

    String EXAMPLE_JWT_VC_NO_DATES = """
            eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkaWQ6ZXhhbXBsZTplYmZlYjFmNzEyZWJjNmYxYzI3NmUxMmVjMjEiLCJqdGk
            iOiJodHRwOi8vZXhhbXBsZS5lZHUvY3JlZGVudGlhbHMvMzczMiIsImlzcyI6Imh0dHBzOi8vZXhhbXBsZS5jb20va2V5cy9mb28uandrIiw
            ibmJmIjoxNTQxNDkzNzI0LCJleHAiOjE1NzMwMjk3MjMsIm5vbmNlIjoiNjYwITYzNDVGU2VyIiwidmMiOnsiQGNvbnRleHQiOlsiaHR0cHM
            6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy9leGFtcGxlcy9
            2MSJdLCJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiVW5pdmVyc2l0eURlZ3JlZUNyZWRlbnRpYWwiXSwiY3JlZGVudGlhbFN1Ymp
            lY3QiOnsiZGVncmVlIjp7InR5cGUiOiJCYWNoZWxvckRlZ3JlZSIsIm5hbWUiOiI8c3BhbiBsYW5nPSdmci1DQSc-QmFjY2FsYXVyw6lhdCB
            lbiBtdXNpcXVlcyBudW3DqXJpcXVlczwvc3Bhbj4ifX19fQ.rm37S4oLVa6j2gc-LGHqJj5G9Yph2b9cyN7DEpwvy_4
            """;

    String EXAMPLE_JWT_VC_NO_NBF = """
            eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkaWQ6ZXhhbXBsZTplYmZlYjFmNzEyZWJjNmYxYzI3NmUxMmVjMjEiLCJqdGk
            iOiJodHRwOi8vZXhhbXBsZS5lZHUvY3JlZGVudGlhbHMvMzczMiIsImlzcyI6Imh0dHBzOi8vZXhhbXBsZS5jb20va2V5cy9mb28uandrIiw
            iZXhwIjoxNTczMDI5NzIzLCJub25jZSI6IjY2MCE2MzQ1RlNlciIsInZjIjp7IkBjb250ZXh0IjpbImh0dHBzOi8vd3d3LnczLm9yZy8yMDE
            4L2NyZWRlbnRpYWxzL3YxIiwiaHR0cHM6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvZXhhbXBsZXMvdjEiXSwidHlwZSI6WyJWZXJ
            pZmlhYmxlQ3JlZGVudGlhbCIsIlVuaXZlcnNpdHlEZWdyZWVDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7ImRlZ3JlZSI6eyJ
            0eXBlIjoiQmFjaGVsb3JEZWdyZWUiLCJuYW1lIjoiPHNwYW4gbGFuZz0nZnItQ0EnPkJhY2NhbGF1csOpYXQgZW4gbXVzaXF1ZXMgbnVtw6l
            yaXF1ZXM8L3NwYW4-In19fX0.mEU-6y1RYGbpxubTTOyq53fJgp5yQvVmly-fXD-vT44
            """;

    String EXAMPLE_ENVELOPED_PRESENTATION = """
            eyJraWQiOiJ2cC1zaWduIiwiYWxnIjoiRVMyNTYifQ.eyJpZCI6ImRhdGE6YXBwbGljYXRpb24vdn
            Arand0LGV5SnJhV1FpT2lKMmNDMXphV2R1SWl3aVlXeG5Jam9pUlZNeU5UWWlmUS5leUowZVhCbEl
            qb2lWbVZ5YVdacFlXSnNaVkJ5WlhObGJuUmhkR2x2YmlJc0lrQmpiMjUwWlhoMElqcGJJbWgwZEhC
            ek9pOHZkM2QzTG5jekxtOXlaeTl1Y3k5amNtVmtaVzUwYVdGc2N5OTJNaUlzSW1oMGRIQnpPaTh2Z
            DNkM0xuY3pMbTl5Wnk5dWN5OWpjbVZrWlc1MGFXRnNjeTlsZUdGdGNHeGxjeTkyTWlKZExDSjJaWE
            pwWm1saFlteGxRM0psWkdWdWRHbGhiQ0k2VzNzaVFHTnZiblJsZUhRaU9pSm9kSFJ3Y3pvdkwzZDN
            keTUzTXk1dmNtY3Zibk12WTNKbFpHVnVkR2xoYkhNdmRqSWlMQ0pwWkNJNkltUmhkR0U2WVhCd2JH
            bGpZWFJwYjI0dmRtTXJhbmQwTEdWNVNuSmhWMUZwVDJsS01sbDVNWHBoVjJSMVNXbDNhVmxYZUc1S
            mFtOXBVbFpOZVU1VVdXbG1VUzVsZVVwcVkyMVdhMXBYTlRCaFYwWnpWVEpPYjFwWE1XaEphbkJpWl
            hsS2NGcERTVFpKYldnd1pFaENlazlwT0haYVdHaG9ZbGhDYzFwVE5YWmpiV04yV2xob2FHSllRbk5
            hV0UxMldrZFdibU50Vm14TWJYQjZZakkwYVV4RFNqQmxXRUpzU1dwdmFWTnVUblppYkU1cVlVZFdk
            RmxUU2psTVNITnBZVmRSYVU5cFNtOWtTRkozWTNwdmRrd3lWalJaVnpGM1lrZFZkV0l6U201TU1sW
            TBXVmN4ZDJKSFZucE1Na1p6WkZjeGRXRlROWEZqTWpsMVNXbDNhV1JJYkhkYVUwazJTV3R3ZW1JeU
            5WUlpNbWhzWWxkRmFXWldNSE5KYlU1NVdsZFNiR0p1VW5CWlYzaFVaRmRLY1ZwWFRqQkphbkEzU1c
            xc2EwbHFiMmxhUjJ4clQyMVdORmxYTVhkaVIxVTJXbGRLYlZwWFNYaGFhbU40VFcxV2FWbDZXbTFO
            VjAxNVRucGFiRTFVU214WmVrbDRTV2wzYVZwSFZtNWpiVlpzU1dwd04wbHVValZqUjFWcFQybEtSb
            VZIUm5SalIzaHNVVzFHYW1GSFZuTmlNMHBGV2xka2VWcFhWV2xNUTBwMVdWY3hiRWxxYjJsUmJVWn
            FZVWRXYzJJelNXZGlNbGxuVlRKT2NGcFhOV3BhVTBKb1ltMVJaMUZZU2pCamVVbzVURU5LYUdKSVZ
            uUmliV3hRV21sSk5tVjVTblZaVnpGc1NXcHZhVkpZYUdoaVdFSnpXbE5DVm1KdGJESmFXRXA2WVZo
            U05VbHVNVGxNUTBwd1drTkpOa2x0YURCa1NFRTJUSGs1TVdKdGJESmFXRXA2WVZoU05VeHRWalJaV
            npGM1lrZFZkbGt6U214YVIxWjFaRWRzYUdKSVRYWk5lbU42VFdsSmMwbHVXbWhpUjJ4clVtNUtkbU
            pUU1RaSmFrbDNUVlJCZEUxRVJYUk5SRVpWVFZSck5rMXFUVFpOYWxKaFNXbDNhV1JJYkhkYVUwazJ
            WM2xLVjFwWVNuQmFiV3hvV1cxNGJGRXpTbXhhUjFaMVpFZHNhR0pEU1hOSmExWTBXVmN4ZDJKSFZr
            VmFWMlI1V2xkV1JHTnRWbXRhVnpVd1lWZEdjMGxwZDJsU1dHaG9ZbGhDYzFwV1FteGpiazUyWW10T
            2VWcFhVbXhpYmxKd1dWZDNhVmhUZDJsUlIwNTJZbTVTYkdWSVVXbFBiSE5wWVVoU01HTklUVFpNZV
            RrelpETmpkV1I2VFhWaU0wcHVUREkxZWt3eVRubGFWMUpzWW01U2NGbFhlSHBNTTFsNVNXbDNhV0Z
            JVWpCalNFMDJUSGs1TTJRelkzVmtlazExWWpOS2Jrd3lOWHBNTWs1NVdsZFNiR0p1VW5CWlYzaDZU
            REpXTkZsWE1YZGlSMVo2VEROWmVVbHNNSE5KYld4Nll6TldiR05wU1RaSmJXZ3daRWhDZWs5cE9IW
            mtWelZ3WkcxV2VXTXliREJsVXpWc1pVZEdkR05IZUd4TU1teDZZek5XYkdOdVRYWk5WRkZwWmxFdV
            oxTnlWVTQxYlU4d2Eyd3hRbUZUZDFwNFJHSnZYMUJJVGtjeGQyMW1NVzExZERKMFZFaENUbGxKV21
            WWFVHSk9YM3BmU0MwNVpHdFBNbVJtWTFBNU0xZEtkM2xGTldWdlNqVnJjVjlxVEhwVFltOVdlbWNp
            TENKMGVYQmxJam9pUlc1MlpXeHZjR1ZrVm1WeWFXWnBZV0pzWlVOeVpXUmxiblJwWVd3aWZTeDdJa
            0JqYjI1MFpYaDBJam9pYUhSMGNITTZMeTkzZDNjdWR6TXViM0puTDI1ekwyTnlaV1JsYm5ScFlXeH
            pMM1l5SWl3aWFXUWlPaUprWVhSaE9tRndjR3hwWTJGMGFXOXVMM1pqSzJwM2RDeGxlVXB5WVZkUmF
            VOXBTakpaZVRGNllWZGtkVWxwZDJsWlYzaHVTV3B2YVZKV1RYbE9WRmxwWmxFdVpYbEthbU50Vm10
            YVZ6VXdZVmRHYzFVeVRtOWFWekZvU1dwd1ltVjVTbkJhUTBrMlNXMW9NR1JJUW5wUGFUaDJXbGhvY
            UdKWVFuTmFVelYyWTIxamRscFlhR2hpV0VKeldsaE5kbHBIVm01amJWWnNURzF3ZW1JeU5HbE1RMG
            93WlZoQ2JFbHFiMmxUYms1MllteE9hbUZIVm5SWlUwbzVURWh6YVdGWFVXbFBhVXB2WkVoU2QyTjZ
            iM1pNTWxZMFdWY3hkMkpIVlhWaU0wcHVUREpXTkZsWE1YZGlSMVo2VERKR2MyUlhNWFZoVXpWeFl6
            STVkVWxwZDJsa1NHeDNXbE5KTmtscmNIcGlNalZVV1RKb2JHSlhSV2xtVmpCelNXMU9lVnBYVW14a
            WJsSndXVmQ0VkdSWFNuRmFWMDR3U1dwd04wbHRiR3RKYW05cFdrZHNhMDl0VmpSWlZ6RjNZa2RWTm
            xwWFNtMWFWMGw0V21wamVFMXRWbWxaZWxwdFRWZE5lVTU2V214TlZFcHNXWHBKZUVscGQybGFSMVp
            1WTIxV2JFbHFjRGRKYmxJMVkwZFZhVTlwU2tabFIwWjBZMGQ0YkZGdFJtcGhSMVp6WWpOS1JWcFha
            SGxhVjFWcFRFTktkVmxYTVd4SmFtOXBVVzFHYW1GSFZuTmlNMGxuWWpKWloxVXlUbkJhVnpWcVdsT
            kNhR0p0VVdkUldFb3dZM2xLT1V4RFNtaGlTRlowWW0xc1VGcHBTVFpsZVVwMVdWY3hiRWxxYjJsU1
            dHaG9ZbGhDYzFwVFFsWmliV3d5V2xoS2VtRllValZKYmpFNVRFTktjRnBEU1RaSmJXZ3daRWhCTmt
            4NU9URmliV3d5V2xoS2VtRllValZNYlZZMFdWY3hkMkpIVlhaWk0wcHNXa2RXZFdSSGJHaGlTRTEy
            VFhwamVrMXBTWE5KYmxwb1lrZHNhMUp1U25aaVUwazJTV3BKZDAxVVFYUk5SRVYwVFVSR1ZVMVVhe
            lpOYWswMlRXcFNZVWxwZDJsa1NHeDNXbE5KTmxkNVNsZGFXRXB3V20xc2FGbHRlR3hSTTBwc1drZF
            dkV1JIYkdoaVEwbHpTV3RXTkZsWE1YZGlSMVpGV2xka2VWcFhWa1JqYlZacldsYzFNR0ZYUm5OSmF
            YZHBVbGhvYUdKWVFuTmFWa0pzWTI1T2RtSnJUbmxhVjFKc1ltNVNjRmxYZDJsWVUzZHBVVWRPZG1K
            dVVteGxTRkZwVDJ4emFXRklVakJqU0UwMlRIazVNMlF6WTNWa2VrMTFZak5LYmt3eU5YcE1NazU1V
            2xkU2JHSnVVbkJaVjNoNlRETlplVWxwZDJsaFNGSXdZMGhOTmt4NU9UTmtNMk4xWkhwTmRXSXpTbT
            VNTWpWNlRESk9lVnBYVW14aWJsSndXVmQ0ZWt3eVZqUlpWekYzWWtkV2Vrd3pXWGxKYkRCelNXMXN
            lbU16Vm14amFVazJTVzFvTUdSSVFucFBhVGgyWkZjMWNHUnRWbmxqTW13d1pWTTFiR1ZIUm5SalIz
            aHNUREpzZW1NelZteGpiazEyVFZSUmFXWlJMbWRUY2xWT05XMVBNR3RzTVVKaFUzZGFlRVJpYjE5U
            VNFNUhNWGR0WmpGdGRYUXlkRlJJUWs1WlNWcGxWMUJpVGw5NlgwZ3RPV1JyVHpKa1ptTlFPVE5YU2
            5kNVJUVmxiMG8xYTNGZmFreDZVMkp2Vm5wbklpd2lkSGx3WlNJNklrVnVkbVZzYjNCbFpGWmxjbWx
            tYVdGaWJHVkRjbVZrWlc1MGFXRnNJbjFkZlEuWXBxYldPZ1A3aFBfLWd2YTlUYzl1N1ZSTFhnSjI1
            WWx5UjhaTVNrRU1oQVkzQmJoclFDeEVQT3YzaTEyc1dUUDd3U2gtN2Nhak92Q2JlTk8zTDhzRGciL
            CJ0eXBlIjoiRW52ZWxvcGVkVmVyaWZpYWJsZVByZXNlbnRhdGlvbiIsIkBjb250ZXh0IjpbImh0dH
            BzOi8vd3d3LnczLm9yZy9ucy9jcmVkZW50aWFscy92MiIsImh0dHBzOi8vd3d3LnczLm9yZy9ucy9
            jcmVkZW50aWFscy9leGFtcGxlcy92MiJdfQ.5HZ1dfEachsqXkX9WvWjemyhsq6ZYztqz5H2BZOFW
            4zlR7RIfw50Piz83Q8lRqnH5ofBRLsXl4Dvk7k4mwJuUA
            """;
    String EXAMPLE_ENVELOPED_PRESENTATION_VC_NOT_ENVELOPED = """
            eyJraWQiOiJ2cC1zaWduIiwiYWxnIjoiRVMyNTYifQ.eyJpZCI6ImRhdGE6YXBwbGljYXRpb24vdn
            Arand0LGV5SnJhV1FpT2lKMmNDMXphV2R1SWl3aVlXeG5Jam9pUlZNeU5UWWlmUS5leUowZVhCbEl
            qb2lWbVZ5YVdacFlXSnNaVkJ5WlhObGJuUmhkR2x2YmlJc0lrQmpiMjUwWlhoMElqcGJJbWgwZEhC
            ek9pOHZkM2QzTG5jekxtOXlaeTl1Y3k5amNtVmtaVzUwYVdGc2N5OTJNaUlzSW1oMGRIQnpPaTh2Z
            DNkM0xuY3pMbTl5Wnk5dWN5OWpjbVZrWlc1MGFXRnNjeTlsZUdGdGNHeGxjeTkyTWlKZExDSjJaWE
            pwWm1saFlteGxRM0psWkdWdWRHbGhiQ0k2V3lKbGVVcHlZVmRSYVU5cFNqSlplVEY2WVZka2RVbHB
            kMmxaVjNodVNXcHZhVkpXVFhsT1ZGbHBabEV1WlhsS2FtTnRWbXRhVnpVd1lWZEdjMVV5VG05YVZ6
            Rm9TV3B3WW1WNVNuQmFRMGsyU1cxb01HUklRbnBQYVRoMldsaG9hR0pZUW5OYVV6VjJZMjFqZGxwW
            WFHaGlXRUp6V2xoTmRscEhWbTVqYlZac1RHMXdlbUl5TkdsTVEwb3daVmhDYkVscWIybFRiazUyWW
            14T2FtRkhWblJaVTBvNVRFaHphV0ZYVVdsUGFVcHZaRWhTZDJONmIzWk1NbFkwV1ZjeGQySkhWWFZ
            pTTBwdVRESldORmxYTVhkaVIxWjZUREpHYzJSWE1YVmhVelZ4WXpJNWRVbHBkMmxrU0d4M1dsTkpO
            a2xyY0hwaU1qVlVXVEpvYkdKWFJXbG1WakJ6U1cxT2VWcFhVbXhpYmxKd1dWZDRWR1JYU25GYVYwN
            HdTV3B3TjBsdGJHdEphbTlwV2tkc2EwOXRWalJaVnpGM1lrZFZObHBYU20xYVYwbDRXbXBqZUUxdF
            ZtbFplbHB0VFZkTmVVNTZXbXhOVkVwc1dYcEplRWxwZDJsYVIxWnVZMjFXYkVscWNEZEpibEkxWTB
            kVmFVOXBTa1psUjBaMFkwZDRiRkZ0Um1waFIxWnpZak5LUlZwWFpIbGFWMVZwVEVOS2RWbFhNV3hK
            YW05cFVXMUdhbUZIVm5OaU0wbG5ZakpaWjFVeVRuQmFWelZxV2xOQ2FHSnRVV2RSV0Vvd1kzbEtPV
            XhEU21oaVNGWjBZbTFzVUZwcFNUWmxlVXAxV1ZjeGJFbHFiMmxTV0dob1lsaENjMXBUUWxaaWJXd3
            lXbGhLZW1GWVVqVkpiakU1VEVOS2NGcERTVFpKYldnd1pFaEJOa3g1T1RGaWJXd3lXbGhLZW1GWVV
            qVk1iVlkwV1ZjeGQySkhWWFpaTTBwc1drZFdkV1JIYkdoaVNFMTJUWHBqZWsxcFNYTkpibHBvWWtk
            c2ExSnVTblppVTBrMlNXcEpkMDFVUVhSTlJFVjBUVVJHVlUxVWF6Wk5hazAyVFdwU1lVbHBkMmxrU
            0d4M1dsTkpObGQ1U2xkYVdFcHdXbTFzYUZsdGVHeFJNMHBzV2tkV2RXUkhiR2hpUTBselNXdFdORm
            xYTVhkaVIxWkZXbGRrZVZwWFZrUmpiVlpyV2xjMU1HRlhSbk5KYVhkcFVsaG9hR0pZUW5OYVZrSnN
            ZMjVPZG1KclRubGFWMUpzWW01U2NGbFhkMmxZVTNkcFVVZE9kbUp1VW14bFNGRnBUMnh6YVdGSVVq
            QmpTRTAyVEhrNU0yUXpZM1ZrZWsxMVlqTktia3d5TlhwTU1rNTVXbGRTYkdKdVVuQlpWM2g2VEROW
            mVVbHBkMmxoU0ZJd1kwaE5Oa3g1T1ROa00yTjFaSHBOZFdJelNtNU1NalY2VERKT2VWcFhVbXhpYm
            xKd1dWZDRla3d5VmpSWlZ6RjNZa2RXZWt3eldYbEpiREJ6U1cxc2VtTXpWbXhqYVVrMlNXMW9NR1J
            JUW5wUGFUaDJaRmMxY0dSdFZubGpNbXd3WlZNMWJHVkhSblJqUjNoc1RESnNlbU16Vm14amJrMTJU
            VlJSYVdaUkxubEhiWGxVWVhadWNXbExMWE5DTFZWQ1VVaE1XVk55UTJkcldUWklPVFJXZUZSd1JsW
            kNkM1ZNV0cxSVVITk5ka0Z1UjBGcmRGOXZMUzF4Wm5nNGFXdE5VWGx2YjBObFRIUkVjRUZ4VldjMk
            xXVmhja1JCSWl3aVpYbEtjbUZYVVdsUGFVb3lXWGt4ZW1GWFpIVkphWGRwV1ZkNGJrbHFiMmxTVms
            xNVRsUlphV1pSTG1WNVNtcGpiVlpyV2xjMU1HRlhSbk5WTWs1dldsY3hhRWxxY0dKbGVVcHdXa05K
            TmtsdGFEQmtTRUo2VDJrNGRscFlhR2hpV0VKeldsTTFkbU50WTNaYVdHaG9ZbGhDYzFwWVRYWmFSM
            Vp1WTIxV2JFeHRjSHBpTWpScFRFTktNR1ZZUW14SmFtOXBVMjVPZG1Kc1RtcGhSMVowV1ZOS09VeE
            ljMmxoVjFGcFQybEtiMlJJVW5kamVtOTJUREpXTkZsWE1YZGlSMVYxWWpOS2Jrd3lWalJaVnpGM1l
            rZFdla3d5Um5Oa1Z6RjFZVk0xY1dNeU9YVkphWGRwWkVoc2QxcFRTVFpKYTNCNllqSTFWRmt5YUd4
            aVYwVnBabFl3YzBsdFRubGFWMUpzWW01U2NGbFhlRlJrVjBweFdsZE9NRWxxY0RkSmJXeHJTV3B2Y
            VZwSGJHdFBiVlkwV1ZjeGQySkhWVFphVjBwdFdsZEplRnBxWTNoTmJWWnBXWHBhYlUxWFRYbE9lbH
            BzVFZSS2JGbDZTWGhKYVhkcFdrZFdibU50Vm14SmFuQTNTVzVTTldOSFZXbFBhVXBHWlVkR2RHTkh
            lR3hSYlVacVlVZFdjMkl6U2tWYVYyUjVXbGRWYVV4RFNuVlpWekZzU1dwdmFWRnRSbXBoUjFaellq
            TkpaMkl5V1dkVk1rNXdXbGMxYWxwVFFtaGliVkZuVVZoS01HTjVTamxNUTBwb1lraFdkR0p0YkZCY
            WFVazJaWGxLZFZsWE1XeEphbTlwVWxob2FHSllRbk5hVTBKV1ltMXNNbHBZU25waFdGSTFTVzR4T1
            V4RFNuQmFRMGsyU1cxb01HUklRVFpNZVRreFltMXNNbHBZU25waFdGSTFURzFXTkZsWE1YZGlSMVY
            yV1ROS2JGcEhWblZrUjJ4b1lraE5kazE2WTNwTmFVbHpTVzVhYUdKSGJHdFNia3AyWWxOSk5rbHFT
            WGROVkVGMFRVUkZkRTFFUmxWTlZHczJUV3BOTmsxcVVtRkphWGRwWkVoc2QxcFRTVFpYZVVwWFdsa
            EtjRnB0YkdoWmJYaHNVVE5LYkZwSFZuVmtSMnhvWWtOSmMwbHJWalJaVnpGM1lrZFdSVnBYWkhsYV
            YxWkVZMjFXYTFwWE5UQmhWMFp6U1dsM2FWSllhR2hpV0VKeldsWkNiR051VG5aaWEwNTVXbGRTYkd
            KdVVuQlpWM2RwV0ZOM2FWRkhUblppYmxKc1pVaFJhVTlzYzJsaFNGSXdZMGhOTmt4NU9UTmtNMk4x
            WkhwTmRXSXpTbTVNTWpWNlRESk9lVnBYVW14aWJsSndXVmQ0ZWt3eldYbEphWGRwWVVoU01HTklUV
            FpNZVRrelpETmpkV1I2VFhWaU0wcHVUREkxZWt3eVRubGFWMUpzWW01U2NGbFhlSHBNTWxZMFdWY3
            hkMkpIVm5wTU0xbDVTV3d3YzBsdGJIcGpNMVpzWTJsSk5rbHRhREJrU0VKNlQyazRkbVJYTlhCa2J
            WWjVZekpzTUdWVE5XeGxSMFowWTBkNGJFd3liSHBqTTFac1kyNU5kazFVVVdsbVVTNTVSMjE1VkdG
            MmJuRnBTeTF6UWkxVlFsRklURmxUY2tObmExazJTRGswVm5oVWNFWldRbmQxVEZodFNGQnpUWFpCY
            mtkQmEzUmZieTB0Y1daNE9HbHJUVkY1YjI5RFpVeDBSSEJCY1ZWbk5pMWxZWEpFUVNKZGZRLm5zNm
            I0bVJ0SUdOSmxBYTR3TkMtMjJaN2pXanhmOFlqNG5YNk9nanVibGIwOFY4LUdvNll4eWkyal9QYVJ
            Ja0sxbDNwYld3Q3ctbzhtRWl3aTExMVF3IiwidHlwZSI6IkVudmVsb3BlZFZlcmlmaWFibGVQcmVz
            ZW50YXRpb24iLCJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvd
            jIiLCJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvZXhhbXBsZXMvdjIiXX0.NMIUGy
            L07clQVAlvy-drJJ5dkmV5VTvThC0azqNfcmhQ1VRSgi0Z4gdYaM92IrW3OiRH3kIR-h7pLR3505m8CQ
            """;

    @NotNull String EXAMPLE_JWT_VC_2_0 = """
            eyJraWQiOiJFeEhrQk1XOWZtYmt2VjI2Nm1ScHVQMnNVWV9OX0VXSU4xbGFwVXpPOHJvIiwiYWxnI
            joiRVMyNTYifQ .eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbH
            MvdjIiLCJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvZXhhbXBsZXMvdjIiXSwiaWQ
            iOiJodHRwOi8vdW5pdmVyc2l0eS5leGFtcGxlL2NyZWRlbnRpYWxzLzM3MzIiLCJ0eXBlIjpbIlZl
            cmlmaWFibGVDcmVkZW50aWFsIiwiRXhhbXBsZURlZ3JlZUNyZWRlbnRpYWwiLCJFeGFtcGxlUGVyc
            29uQ3JlZGVudGlhbCJdLCJpc3N1ZXIiOiJodHRwczovL3VuaXZlcnNpdHkuZXhhbXBsZS9pc3N1ZX
            JzLzE0IiwidmFsaWRGcm9tIjoiMjAxMC0wMS0wMVQxOToyMzoyNFoiLCJjcmVkZW50aWFsU3ViamV
            jdCI6eyJpZCI6ImRpZDpleGFtcGxlOmViZmViMWY3MTJlYmM2ZjFjMjc2ZTEyZWMyMSIsImRlZ3Jl
            ZSI6eyJ0eXBlIjoiRXhhbXBsZUJhY2hlbG9yRGVncmVlIiwibmFtZSI6IkJhY2hlbG9yIG9mIFNja
            WVuY2UgYW5kIEFydHMifSwiYWx1bW5pT2YiOnsibmFtZSI6IkV4YW1wbGUgVW5pdmVyc2l0eSJ9fS
            wiY3JlZGVudGlhbFNjaGVtYSI6W3siaWQiOiJodHRwczovL2V4YW1wbGUub3JnL2V4YW1wbGVzL2R
            lZ3JlZS5qc29uIiwidHlwZSI6Ikpzb25TY2hlbWEifSx7ImlkIjoiaHR0cHM6Ly9leGFtcGxlLm9y
            Zy9leGFtcGxlcy9hbHVtbmkuanNvbiIsInR5cGUiOiJKc29uU2NoZW1hIn1dfQ .ZO5PfcjJ7aq-F
            FgFnvKF4irU-4Cv6_zLTGt7t7rVhb_K-veUd3XKTNbfiS_RtedrNYgay8PksZfTnkk2gnNFSw
            """;
}
