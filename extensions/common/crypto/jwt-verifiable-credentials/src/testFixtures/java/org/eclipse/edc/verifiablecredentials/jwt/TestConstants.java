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
    String CENTRAL_ISSUER_KEY_ID = "central-issuer-key1";
    String PRESENTER_KEY_ID = "my-key1";
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

    // example generated with Claude Code/Sonnet 4.6
    String VP_EXAMPLE_VCDM11 = """
            eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImRpZDpleGFtcGxlOjB4YWJjI2tleTEifQ.eyJpc3MiOiJkaWQ6ZXhhbXBsZTplY
            mZlYjFmNzEyZWJjNmYxYzI3NmUxMmVjMjEiLCJqdGkiOiJ1cm46dXVpZDozOTc4MzQ0Zi04NTk2LTRjM2EtYTk3OC04ZmNhYmEzOTAzYzUiL
            CJhdWQiOiJkaWQ6ZXhhbXBsZTo0YTU3NTQ2OTczNDM2ZjZmNmM0YTRhNTc1NzMiLCJuYmYiOjE1NDE0OTM3MjQsImlhdCI6MTU0MTQ5MzcyN
            CwiZXhwIjoxNTczMDI5NzIzLCJub25jZSI6IjM0M3MkRlNGRGEtIiwidnAiOnsiQGNvbnRleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzIwM
            TgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy9leGFtcGxlcy92MSJdLCJ0eXBlIjpbIlZlc
            mlmaWFibGVQcmVzZW50YXRpb24iLCJDcmVkZW50aWFsTWFuYWdlclByZXNlbnRhdGlvbiJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6WyJle
            UpoYkdjaU9pSlNVekkxTmlJc0luUjVjQ0k2SWtwWFZDSXNJbXRwWkNJNkltUnBaRHBsZUdGdGNHeGxPbUZpWm1VeE0yWTNNVEl4TWpBME16R
            mpNamMyWlRFeVpXTmhZaU5yWlhsekxURWlmUS5leUp6ZFdJaU9pSmthV1E2WlhoaGJYQnNaVHBsWW1abFlqRm1OekV5WldKak5tWXhZekkzT
            m1VeE1tVmpNakVpTENKcWRHa2lPaUpvZEhSd09pOHZaWGhoYlhCc1pTNWxaSFV2WTNKbFpHVnVkR2xoYkhNdk16Y3pNaUlzSW1semN5STZJb
            WgwZEhCek9pOHZaWGhoYlhCc1pTNWpiMjB2YTJWNWN5OW1iMjh1YW5kcklpd2libUptSWpveE5UUXhORGt6TnpJMExDSnBZWFFpT2pFMU5ER
            TBPVE0zTWpRc0ltVjRjQ0k2TVRVM016QXlPVGN5TXl3aWJtOXVZMlVpT2lJMk5qQWhOak0wTlVaVFpYSWlMQ0oyWXlJNmV5SkFZMjl1ZEdWN
            GRDSTZXeUpvZEhSd2N6b3ZMM2QzZHk1M015NXZjbWN2TWpBeE9DOWpjbVZrWlc1MGFXRnNjeTkyTVNJc0ltaDBkSEJ6T2k4dmQzZDNMbmN6T
            G05eVp5OHlNREU0TDJOeVpXUmxiblJwWVd4ekwyVjRZVzF3YkdWekwzWXhJbDBzSW5SNWNHVWlPbHNpVm1WeWFXWnBZV0pzWlVOeVpXUmxib
            lJwWVd3aUxDSlZibWwyWlhKemFYUjVSR1ZuY21WbFEzSmxaR1Z1ZEdsaGJDSmRMQ0pqY21Wa1pXNTBhV0ZzVTNWaWFtVmpkQ0k2ZXlKa1pXZ
            HlaV1VpT25zaWRIbHdaU0k2SWtKaFkyaGxiRzl5UkdWbmNtVmxJaXdpYm1GdFpTSTZJanh6Y0dGdUlHeGhibWM5SjJaeUxVTkJKejVDWVdOa
            llXeGhkWExEcVdGMElHVnVJRzExYzJseGRXVnpJRzUxYmNPcGNtbHhkV1Z6UEM5emNHRnVQaUo5ZlgxOS5LTEpvNUdBeUJORDNMRFRuOUg3R
            lFva0VzVUVpOGpLd1hoR3ZvTjNKdFJhNTF4ck5EZ1hEYjBjcTFVVFlCLXJLNEZ0OVlWbVIxTklfWk9GOG9HY183d0FwOFBIYkYySGFXb2RRS
            W9PQnh4VC00V05xQXhmdDdFVDZsa0gtNFM2VXgzclNHQW1jek1vaEVFZjhlQ2VOLWpDOFdla2RQbDZ6S1pRajBZUEIxcng2WDAteGxGQnM3Y
            2w2V3Q4cmZCUF90WjlZZ1ZXclFtVVd5cFNpb2MwTVV5aXBobXlFYkxaYWdUeVBsVXlmbEdsRWRxclpBdjZlU2U2UnR4Snk2TTEtbEQ3YTVIV
            HphbllUV0JQQVVIRFpHeUdLWGRKdy1XX3gwSVdDaEJ6STh0M2twRzI1M2ZnNlYzdFBnSGVLWEU5NGZ6X1FwWWZnLS03a0xzeUJBZlFHYmciX
            X19.ft_Eq4IniBrr7gtzRfrYj8Vy1aPXuFZU-6_ai0wvaKcsrzI4JkQEKTvbJwdvIeuGuTqy7ipO-EYi7V4TvonPuTRdpB7ZHOlYlbZ4wA9W
            J6mSVSqDACvYRiFvrOFmie8rgm6GacWatgO4m4NqiFKFko3r58LueFfGw47NK9RcfOkVQeHCq4btaDqksDKeoTrNysF4YS89INa-prWomrLR
            AhnwLOo1Etp3E4ESAxg73CR2kA5AoMbf5KtFueWnMcSbQkMRdWcGC1VssC0tB0JffVjq7ZV6OTyV4kl1-UVgiPLXUTpupFfLRhf9QpqMBjYg
            P62KvhIvW8BbkGUelYMetAthos
            """;

    // example taken from here: https://www.w3.org/TR/vc-jose-cose/#example-a-simple-example-of-a-verifiable-presentation-secured-with-jose-with-the-envelopedverifiablecredential-type
    String VP_SIMPLE_JOSE_ENVELOPED_CREDENTIAL = """
            eyJraWQiOiJFeEhrQk1XOWZtYmt2VjI2Nm1ScHVQMnNVWV9OX0VXSU4xbGFwVXpPOHJvIiwiYWxnIjoiRVMyNTYifQ .eyJAY29udGV4dCI6
            WyJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvdjIiLCJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvZXhhbXBs
            ZXMvdjIiXSwidHlwZSI6IlZlcmlmaWFibGVQcmVzZW50YXRpb24iLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6W3siQGNvbnRleHQiOiJodHRw
            czovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvdjIiLCJpZCI6ImRhdGE6YXBwbGljYXRpb24vdmMrand0LGV5SnJhV1FpT2lKRmVFaHJR
            azFYT1dadFltdDJWakkyTm0xU2NIVlFNbk5WV1Y5T1gwVlhTVTR4YkdGd1ZYcFBPSEp2SWl3aVlXeG5Jam9pUlZNek9EUWlmUS5leUpBWTI5
            dWRHVjRkQ0k2V3lKb2RIUndjem92TDNkM2R5NTNNeTV2Y21jdmJuTXZZM0psWkdWdWRHbGhiSE12ZGpJaUxDSm9kSFJ3Y3pvdkwzZDNkeTUz
            TXk1dmNtY3Zibk12WTNKbFpHVnVkR2xoYkhNdlpYaGhiWEJzWlhNdmRqSWlYU3dpYVdRaU9pSm9kSFJ3T2k4dmRXNXBkbVZ5YzJsMGVTNWxl
            R0Z0Y0d4bEwyTnlaV1JsYm5ScFlXeHpMekU0TnpJaUxDSjBlWEJsSWpwYklsWmxjbWxtYVdGaWJHVkRjbVZrWlc1MGFXRnNJaXdpUlhoaGJY
            QnNaVUZzZFcxdWFVTnlaV1JsYm5ScFlXd2lYU3dpYVhOemRXVnlJam9pYUhSMGNITTZMeTkxYm1sMlpYSnphWFI1TG1WNFlXMXdiR1V2YVhO
            emRXVnljeTgxTmpVd05Ea2lMQ0oyWVd4cFpFWnliMjBpT2lJeU1ERXdMVEF4TFRBeFZERTVPakl6T2pJMFdpSXNJbU55WldSbGJuUnBZV3hU
            WTJobGJXRWlPbnNpYVdRaU9pSm9kSFJ3Y3pvdkwyVjRZVzF3YkdVdWIzSm5MMlY0WVcxd2JHVnpMMlJsWjNKbFpTNXFjMjl1SWl3aWRIbHda
            U0k2SWtwemIyNVRZMmhsYldFaWZTd2lZM0psWkdWdWRHbGhiRk4xWW1wbFkzUWlPbnNpYVdRaU9pSmthV1E2WlhoaGJYQnNaVG94TWpNaUxD
            SmtaV2R5WldVaU9uc2lkSGx3WlNJNklrSmhZMmhsYkc5eVJHVm5jbVZsSWl3aWJtRnRaU0k2SWtKaFkyaGxiRzl5SUc5bUlGTmphV1Z1WTJV
            Z1lXNWtJRUZ5ZEhNaWZYMTkuZDJrNE8zRnl0UUpmODNrTGgtSHNYdVB2aDZ5ZU9saEpFTFZvNVRGNzFndTdlbHNsUXlPZjJaSXRBWHJ0YlhG
            NEt6OVdpdk5kenRPYXl6NFZVUTBNd2E4eUNEWmtQOUIycEgtOVNfdGNBRnhlb2VKNlo0WG5GdUxfRE9ma1IxZlA7ZGF0YTphcHBsaWNhdGlv
            bi92Yytqd3QsZXlKcmFXUWlPaUpGZUVoclFrMVhPV1p0WW10MlZqSTJObTFTY0hWUU1uTlZXVjlPWDBWWFNVNHhiR0Z3VlhwUE9ISnZJaXdp
            WVd4bklqb2lSVk16T0RRaWZRLmV5SkFZMjl1ZEdWNGRDSTZXeUpvZEhSd2N6b3ZMM2QzZHk1M015NXZjbWN2Ym5NdlkzSmxaR1Z1ZEdsaGJI
            TXZkaklpTENKb2RIUndjem92TDNkM2R5NTNNeTV2Y21jdmJuTXZZM0psWkdWdWRHbGhiSE12WlhoaGJYQnNaWE12ZGpJaVhTd2lhV1FpT2lK
            b2RIUndPaTh2ZFc1cGRtVnljMmwwZVM1bGVHRnRjR3hsTDJOeVpXUmxiblJwWVd4ekx6RTROeklpTENKMGVYQmxJanBiSWxabGNtbG1hV0Zp
            YkdWRGNtVmtaVzUwYVdGc0lpd2lSWGhoYlhCc1pVRnNkVzF1YVVOeVpXUmxiblJwWVd3aVhTd2lhWE56ZFdWeUlqb2lhSFIwY0hNNkx5OTFi
            bWwyWlhKemFYUjVMbVY0WVcxd2JHVXZhWE56ZFdWeWN5ODFOalV3TkRraUxDSjJZV3hwWkVaeWIyMGlPaUl5TURFd0xUQXhMVEF4VkRFNU9q
            SXpPakkwV2lJc0ltTnlaV1JsYm5ScFlXeFRZMmhsYldFaU9uc2lhV1FpT2lKb2RIUndjem92TDJWNFlXMXdiR1V1YjNKbkwyVjRZVzF3YkdW
            ekwyUmxaM0psWlM1cWMyOXVJaXdpZEhsd1pTSTZJa3B6YjI1VFkyaGxiV0VpZlN3aVkzSmxaR1Z1ZEdsaGJGTjFZbXBsWTNRaU9uc2lhV1Fp
            T2lKa2FXUTZaWGhoYlhCc1pUb3hNak1pTENKa1pXZHlaV1VpT25zaWRIbHdaU0k2SWtKaFkyaGxiRzl5UkdWbmNtVmxJaXdpYm1GdFpTSTZJ
            a0poWTJobGJHOXlJRzltSUZOamFXVnVZMlVnWVc1a0lFRnlkSE1pZlgxOS5kMms0TzNGeXRRSmY4M2tMaC1Ic1h1UHZoNnllT2xoSkVMVm81
            VEY3MWd1N2Vsc2xReU9mMlpJdEFYcnRiWEY0S3o5V2l2TmR6dE9heXo0VlVRME13YTh5Q0Raa1A5QjJwSC05U190Y0FGeGVvZUo2WjRYbkZ1
            TF9ET2ZrUjFmUCIsInR5cGUiOiJFbnZlbG9wZWRWZXJpZmlhYmxlQ3JlZGVudGlhbCJ9XX0 .W-VCYKsmbzHlKT13jPIDNqx49jXb5VNyRLV
            I-cNoBy8gOoYpLJgrV1OYtG8BQd5FtU5K7DxYuwY6HxiKM5cpbQ
            """;

    String VP_ENVELOPED_JOSE_ENVELOPED_CREDENTIAL = """
            eyJraWQiOiJFeEhrQk1XOWZtYmt2VjI2Nm1ScHVQMnNVWV9OX0VXSU4xbGFwVXpPOHJvIiwiYWxnIjoiRVMyNTYifQ .eyJAY29udGV4dCI6
            WyJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvdjIiLCJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvZXhhbXBs
            ZXMvdjIiXSwidHlwZSI6IkVudmVsb3BlZFZlcmlmaWFibGVQcmVzZW50YXRpb24iLCJpZCI6ImRhdGE6YXBwbGljYXRpb24vdnArand0LGV5
            SnJhV1FpT2lKRmVFaHJRazFYT1dadFltdDJWakkyTm0xU2NIVlFNbk5WV1Y5T1gwVlhTVTR4YkdGd1ZYcFBPSEp2SWl3aVlXeG5Jam9pUlZN
            eU5UWWlmUS5leUpBWTI5dWRHVjRkQ0k2V3lKb2RIUndjem92TDNkM2R5NTNNeTV2Y21jdmJuTXZZM0psWkdWdWRHbGhiSE12ZGpJaUxDSm9k
            SFJ3Y3pvdkwzZDNkeTUzTXk1dmNtY3Zibk12WTNKbFpHVnVkR2xoYkhNdlpYaGhiWEJzWlhNdmRqSWlYU3dpZEhsd1pTSTZJbFpsY21sbWFX
            RmliR1ZRY21WelpXNTBZWFJwYjI0aUxDSjJaWEpwWm1saFlteGxRM0psWkdWdWRHbGhiQ0k2VzNzaVFHTnZiblJsZUhRaU9pSm9kSFJ3Y3pv
            dkwzZDNkeTUzTXk1dmNtY3Zibk12WTNKbFpHVnVkR2xoYkhNdmRqSWlMQ0pwWkNJNkltUmhkR0U2WVhCd2JHbGpZWFJwYjI0dmRtTXJhbmQw
            TEdWNVNuSmhWMUZwVDJsS1JtVkZhSEpSYXpGWVQxZGFkRmx0ZERKV2Fra3lUbTB4VTJOSVZsRk5iazVXVjFZNVQxZ3dWbGhUVlRSNFlrZEdk
            MVpZY0ZCUFNFcDJTV2wzYVZsWGVHNUphbTlwVWxaTmVrOUVVV2xtVVM1bGVVcEJXVEk1ZFdSSFZqUmtRMGsyVjNsS2IyUklVbmRqZW05MlRE
            TmtNMlI1TlROTmVUVjJZMjFqZG1KdVRYWlpNMHBzV2tkV2RXUkhiR2hpU0UxMlpHcEphVXhEU205a1NGSjNZM3B2ZGt3elpETmtlVFV6VFhr
            MWRtTnRZM1ppYmsxMldUTktiRnBIVm5Wa1IyeG9Za2hOZGxwWWFHaGlXRUp6V2xoTmRtUnFTV2xZVTNkcFlWZFJhVTlwU205a1NGSjNUMms0
            ZG1SWE5YQmtiVlo1WXpKc01HVlROV3hsUjBaMFkwZDRiRXd5VG5sYVYxSnNZbTVTY0ZsWGVIcE1la1UwVG5wSmFVeERTakJsV0VKc1NXcHdZ
            a2xzV214amJXeHRZVmRHYVdKSFZrUmpiVlpyV2xjMU1HRlhSbk5KYVhkcFVsaG9hR0pZUW5OYVZVWnpaRmN4ZFdGVlRubGFWMUpzWW01U2NG
            bFhkMmxZVTNkcFlWaE9lbVJYVm5sSmFtOXBZVWhTTUdOSVRUWk1lVGt4WW0xc01scFlTbnBoV0ZJMVRHMVdORmxYTVhkaVIxVjJZVmhPZW1S
            WFZubGplVGd4VG1wVmQwNUVhMmxNUTBveVdWZDRjRnBGV25saU1qQnBUMmxKZVUxRVJYZE1WRUY0VEZSQmVGWkVSVFZQYWtsNlQycEpNRmRw
            U1hOSmJVNTVXbGRTYkdKdVVuQlpWM2hVV1RKb2JHSlhSV2xQYm5OcFlWZFJhVTlwU205a1NGSjNZM3B2ZGt3eVZqUlpWekYzWWtkVmRXSXpT
            bTVNTWxZMFdWY3hkMkpIVm5wTU1sSnNXak5LYkZwVE5YRmpNamwxU1dsM2FXUkliSGRhVTBrMlNXdHdlbUl5TlZSWk1taHNZbGRGYVdaVGQy
            bFpNMHBzV2tkV2RXUkhiR2hpUms0eFdXMXdiRmt6VVdsUGJuTnBZVmRSYVU5cFNtdGhWMUUyV2xob2FHSllRbk5hVkc5NFRXcE5hVXhEU210
            YVYyUjVXbGRWYVU5dWMybGtTR3gzV2xOSk5rbHJTbWhaTW1oc1lrYzVlVkpIVm01amJWWnNTV2wzYVdKdFJuUmFVMGsyU1d0S2FGa3lhR3hp
            UnpsNVNVYzViVWxHVG1waFYxWjFXVEpWWjFsWE5XdEpSVVo1WkVoTmFXWllNVGt1WkRKck5FOHpSbmwwVVVwbU9ETnJUR2d0U0hOWWRWQjJh
            RFo1WlU5c2FFcEZURlp2TlZSR056Rm5kVGRsYkhOc1VYbFBaakphU1hSQldISjBZbGhHTkV0Nk9WZHBkazVrZW5SUFlYbDZORlpWVVRCTmQy
            RTRlVU5FV210UU9VSXljRWd0T1ZOZmRHTkJSbmhsYjJWS05sbzBXRzVHZFV4ZlJFOW1hMUl4WmxBN1pHRjBZVHBoY0hCc2FXTmhkR2x2Ymk5
            Mll5dHFkM1FzWlhsS2NtRlhVV2xQYVVwR1pVVm9jbEZyTVZoUFYxcDBXVzEwTWxacVNUSk9iVEZUWTBoV1VVMXVUbFpYVmpsUFdEQldXRk5W
            TkhoaVIwWjNWbGh3VUU5SVNuWkphWGRwV1ZkNGJrbHFiMmxTVmsxNlQwUlJhV1pSTG1WNVNrRlpNamwxWkVkV05HUkRTVFpYZVVwdlpFaFNk
            Mk42YjNaTU0yUXpaSGsxTTAxNU5YWmpiV04yWW01TmRsa3pTbXhhUjFaMVpFZHNhR0pJVFhaa2FrbHBURU5LYjJSSVVuZGplbTkyVEROa00y
            UjVOVE5OZVRWMlkyMWpkbUp1VFhaWk0wcHNXa2RXZFdSSGJHaGlTRTEyV2xob2FHSllRbk5hV0UxMlpHcEphVmhUZDJsaFYxRnBUMmxLYjJS
            SVVuZFBhVGgyWkZjMWNHUnRWbmxqTW13d1pWTTFiR1ZIUm5SalIzaHNUREpPZVZwWFVteGlibEp3V1ZkNGVreDZSVFJPZWtscFRFTktNR1ZZ
            UW14SmFuQmlTV3hhYkdOdGJHMWhWMFpwWWtkV1JHTnRWbXRhVnpVd1lWZEdjMGxwZDJsU1dHaG9ZbGhDYzFwVlJuTmtWekYxWVZWT2VWcFhV
            bXhpYmxKd1dWZDNhVmhUZDJsaFdFNTZaRmRXZVVscWIybGhTRkl3WTBoTk5reDVPVEZpYld3eVdsaEtlbUZZVWpWTWJWWTBXVmN4ZDJKSFZY
            WmhXRTU2WkZkV2VXTjVPREZPYWxWM1RrUnJhVXhEU2pKWlYzaHdXa1ZhZVdJeU1HbFBhVWw1VFVSRmQweFVRWGhNVkVGNFZrUkZOVTlxU1hw
            UGFra3dWMmxKYzBsdFRubGFWMUpzWW01U2NGbFhlRlJaTW1oc1lsZEZhVTl1YzJsaFYxRnBUMmxLYjJSSVVuZGplbTkyVERKV05GbFhNWGRp
            UjFWMVlqTktia3d5VmpSWlZ6RjNZa2RXZWt3eVVteGFNMHBzV2xNMWNXTXlPWFZKYVhkcFpFaHNkMXBUU1RaSmEzQjZZakkxVkZreWFHeGlW
            MFZwWmxOM2FWa3pTbXhhUjFaMVpFZHNhR0pHVGpGWmJYQnNXVE5SYVU5dWMybGhWMUZwVDJsS2EyRlhVVFphV0dob1lsaENjMXBVYjNoTmFr
            MXBURU5LYTFwWFpIbGFWMVZwVDI1emFXUkliSGRhVTBrMlNXdEthRmt5YUd4aVJ6bDVVa2RXYm1OdFZteEphWGRwWW0xR2RGcFRTVFpKYTBw
            b1dUSm9iR0pIT1hsSlJ6bHRTVVpPYW1GWFZuVlpNbFZuV1ZjMWEwbEZSbmxrU0UxcFpsZ3hPUzVrTW1zMFR6TkdlWFJSU21ZNE0ydE1hQzFJ
            YzFoMVVIWm9ObmxsVDJ4b1NrVk1WbTgxVkVZM01XZDFOMlZzYzJ4UmVVOW1NbHBKZEVGWWNuUmlXRVkwUzNvNVYybDJUbVI2ZEU5aGVYbzBW
            bFZSTUUxM1lUaDVRMFJhYTFBNVFqSndTQzA1VTE5MFkwRkdlR1Z2WlVvMldqUllia1oxVEY5RVQyWnJVakZtVUNJc0luUjVjR1VpT2lKRmJu
            WmxiRzl3WldSV1pYSnBabWxoWW14bFEzSmxaR1Z1ZEdsaGJDSjlYWDAuRGlaZlh3NWpUWGVEQm9icTVaZGNMM1MzbzhtaW9aSmxxbzNpSER0
            TGNFd3c1TF9uMlpKZkFKVS1hLVNtcXZNWU0tLTd3NENtZU9mcTg5MFVHc2dfYVEiLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6W119 .ndvXgG
            0tEU5qu5B9hoYgQBSExPrjgQSs8mO1Sd62hUVyfeND3Dcym5gFL4gr_rM-_0glipfbNTZK7BxvSoibHw
            """;
    String RANDOM_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30";
}
