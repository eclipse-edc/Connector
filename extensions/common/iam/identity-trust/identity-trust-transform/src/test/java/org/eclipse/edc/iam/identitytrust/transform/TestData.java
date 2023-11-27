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
    String EXAMPLE_JWT_VP = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImRpZDpleGFtcGxlOjB4YWJjI2" +
            "tleTEifQ.eyJpc3MiOiJkaWQ6ZXhhbXBsZTplYmZlYjFmNzEyZWJjNmYxYzI3NmUxMmVjMjEiLCJqdGkiOiJ1cm46dXVpZDozOTc4MzQ0Zi0" +
            "4NTk2LTRjM2EtYTk3OC04ZmNhYmEzOTAzYzUiLCJhdWQiOiJkaWQ6ZXhhbXBsZTo0YTU3NTQ2OTczNDM2ZjZmNmM0YTRhNTc1NzMiLCJuYmY" +
            "iOjE1NDE0OTM3MjQsImlhdCI6MTU0MTQ5MzcyNCwiZXhwIjoxNTczMDI5NzIzLCJub25jZSI6IjM0M3MkRlNGRGEtIiwidnAiOnsiQGNvbn" +
            "RleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50a" +
            "WFscy9leGFtcGxlcy92MSJdLCJ0eXBlIjpbIlZlcmlmaWFibGVQcmVzZW50YXRpb24iLCJDcmVkZW50aWFsTWFuYWdlclByZXNlbnRhdGlv" +
            "biJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6WyJleUpoYkdjaU9pSlNVekkxTmlJc0luUjVjQ0k2SWtwWFZDSXNJbXRwWkNJNkltUnBaRHB" +
            "sZUdGdGNHeGxPbUZpWm1VeE0yWTNNVEl4TWpBME16RmpNamMyWlRFeVpXTmhZaU5yWlhsekxURWlmUS5leUp6ZFdJaU9pSmthV1E2WlhoaG" +
            "JYQnNaVHBsWW1abFlqRm1OekV5WldKak5tWXhZekkzTm1VeE1tVmpNakVpTENKcWRHa2lPaUpvZEhSd09pOHZaWGhoYlhCc1pTNWxaSFV2W" +
            "TNKbFpHVnVkR2xoYkhNdk16Y3pNaUlzSW1semN5STZJbWgwZEhCek9pOHZaWGhoYlhCc1pTNWpiMjB2YTJWNWN5OW1iMjh1YW5kcklpd2lib" +
            "UptSWpveE5UUXhORGt6TnpJMExDSnBZWFFpT2pFMU5ERTBPVE0zTWpRc0ltVjRjQ0k2TVRVM016QXlPVGN5TXl3aWJtOXVZMlVpT2lJMk5qQ" +
            "WhOak0wTlVaVFpYSWlMQ0oyWXlJNmV5SkFZMjl1ZEdWNGRDSTZXeUpvZEhSd2N6b3ZMM2QzZHk1M015NXZjbWN2TWpBeE9DOWpjbVZrWlc1M" +
            "GFXRnNjeTkyTVNJc0ltaDBkSEJ6T2k4dmQzZDNMbmN6TG05eVp5OHlNREU0TDJOeVpXUmxiblJwWVd4ekwyVjRZVzF3YkdWekwzWXhJbDBzS" +
            "W5SNWNHVWlPbHNpVm1WeWFXWnBZV0pzWlVOeVpXUmxiblJwWVd3aUxDSlZibWwyWlhKemFYUjVSR1ZuY21WbFEzSmxaR1Z1ZEdsaGJDSmRM" +
            "Q0pqY21Wa1pXNTBhV0ZzVTNWaWFtVmpkQ0k2ZXlKa1pXZHlaV1VpT25zaWRIbHdaU0k2SWtKaFkyaGxiRzl5UkdWbmNtVmxJaXdpYm1GdFp" +
            "TSTZJanh6Y0dGdUlHeGhibWM5SjJaeUxVTkJKejVDWVdOallXeGhkWExEcVdGMElHVnVJRzExYzJseGRXVnpJRzUxYmNPcGNtbHhkV1Z6UE" +
            "M5emNHRnVQaUo5ZlgxOS5LTEpvNUdBeUJORDNMRFRuOUg3RlFva0VzVUVpOGpLd1hoR3ZvTjNKdFJhNTF4ck5EZ1hEYjBjcTFVVFlCLXJLNE" +
            "Z0OVlWbVIxTklfWk9GOG9HY183d0FwOFBIYkYySGFXb2RRSW9PQnh4VC00V05xQXhmdDdFVDZsa0gtNFM2VXgzclNHQW1jek1vaEVFZjhlQ" +
            "2VOLWpDOFdla2RQbDZ6S1pRajBZUEIxcng2WDAteGxGQnM3Y2w2V3Q4cmZCUF90WjlZZ1ZXclFtVVd5cFNpb2MwTVV5aXBobXlFYkxaYWdUe" +
            "VBsVXlmbEdsRWRxclpBdjZlU2U2UnR4Snk2TTEtbEQ3YTVIVHphbllUV0JQQVVIRFpHeUdLWGRKdy1XX3gwSVdDaEJ6STh0M2twRzI1M2ZnN" +
            "lYzdFBnSGVLWEU5NGZ6X1FwWWZnLS03a0xzeUJBZlFHYmciXX19.ft_Eq4IniBrr7gtzRfrYj8Vy1aPXuFZU-6_ai0wvaKcsrzI4JkQEKTv" +
            "bJwdvIeuGuTqy7ipO-EYi7V4TvonPuTRdpB7ZHOlYlbZ4wA9WJ6mSVSqDACvYRiFvrOFmie8rgm6GacWatgO4m4NqiFKFko3r58LueFfGw4" +
            "7NK9RcfOkVQeHCq4btaDqksDKeoTrNysF4YS89INa-prWomrLRAhnwLOo1Etp3E4ESAxg73CR2kA5AoMbf5KtFueWnMcSbQkMRdWcGC1VssC" +
            "0tB0JffVjq7ZV6OTyV4kl1-UVgiPLXUTpupFfLRhf9QpqMBjYgP62KvhIvW8BbkGUelYMetA";

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
            7EbH9P_HKksL5jVPzLNMHLE68Z907J22n3cthZFVPAgpwFgIcIMAVB7uyOVR0P8PA3JdFQg""";

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
            eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2YyI6eyJAY29udGV4dCI6WyJodHRwczovL3
            d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSIsImh0dHBzOi8vd3d3LnczLm9yZy8yMDE4L
            2NyZWRlbnRpYWxzL2V4YW1wbGVzL3YxIl0sImlkIjoiaHR0cDovL2V4YW1wbGUuZWR1L2NyZWRl
            bnRpYWxzLzM3MzIiLCJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiVW5pdmVyc2l0eUR
            lZ3JlZUNyZWRlbnRpYWwiXSwiaXNzdWVyIjoiaHR0cHM6Ly9leGFtcGxlLmVkdS9pc3N1ZXJzLz
            E0IiwiaXNzdWFuY2VEYXRlIjoiMjAxMC0wMS0wMVQxOToyMzoyNFoiLCJjcmVkZW50aWFsU3Via
            mVjdCI6eyJpZCI6ImRpZDpleGFtcGxlOmViZmViMWY3MTJlYmM2ZjFjMjc2ZTEyZWMyMSIsImRl
            Z3JlZSI6eyJ0eXBlIjoiQmFjaGVsb3JEZWdyZWUiLCJuYW1lIjoiQmFjaGVsb3Igb2YgU2NpZW5
            jZSBhbmQgQXJ0cyJ9fSwiY3JlZGVudGlhbFN0YXR1cyI6eyJpZCI6Imh0dHBzOi8vZXhhbXBsZS
            5lZHUvc3RhdHVzLzI0IiwidHlwZSI6IkNyZWRlbnRpYWxTdGF0dXNMaXN0MjAxNyJ9fSwiaXNzI
            joiaHR0cHM6Ly9leGFtcGxlLmVkdS9pc3N1ZXJzLzE0IiwibmJmIjoxMjYyMzczODA0LCJqdGki
            OiJodHRwOi8vZXhhbXBsZS5lZHUvY3JlZGVudGlhbHMvMzczMiIsInN1YiI6ImRpZDpleGFtcGx
            lOmViZmViMWY3MTJlYmM2ZjFjMjc2ZTEyZWMyMSJ9.YQKQUu_zreDs69AZ8YqpMGHLl9V_tWH4N
            S9P9l67J1wWHf0QCyt5hyuA8ckM4seV-1TRbeiHwdJ3VRkDMcwFcg
            """;
}
