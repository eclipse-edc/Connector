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

package org.eclipse.edc.verifiablecredentials.linkeddata;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;

import java.time.Instant;
import java.util.stream.Stream;

import static jakarta.json.Json.createObjectBuilder;

public interface TestData {

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
    String NAME_CREDENTIAL_ISSUER = "did:web:name-issuer";
    String MEMBERSHIP_CREDENTIAL_ISSUER = "did:web:membership-issuer";

    /**
     * Generates a Verifiable Credential (VC) with the specified issuer, issuance date,
     * credential subject, and types.
     *
     * @param issuer            The issuer of the Verifiable Credential.
     * @param issuanceDate      The issuance date of the Verifiable Credential.
     * @param credentialSubject The credential subject of the Verifiable Credential.
     * @param types             The types of the Verifiable Credential.
     * @return The generated Verifiable Credential as a JSON-LD string.
     */
    static String generateVc(String issuer, Instant issuanceDate, JsonObject credentialSubject, String... types) {
        var typeArray = Json.createArrayBuilder().add("VerifiableCredential");

        Stream.of(types).forEach(typeArray::add);

        return createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, Json.createArrayBuilder()
                        .add("https://www.w3.org/2018/credentials/v1")
                        .add("https://www.w3.org/2018/credentials/examples/v1"))
                .add("type", typeArray)
                .add("issuer", issuer)
                .add("issuanceDate", issuanceDate.toString())
                .add("credentialSubject", credentialSubject)
                .build()
                .toString();
    }

    /**
     * Generates a Verifiable Credential (VC) for a fictional dataspace membership.
     *
     * @return The generated Verifiable Credential as a JSON-LD string.
     */
    static String createMembershipCredential() {
        return generateVc(MEMBERSHIP_CREDENTIAL_ISSUER,
                Instant.parse("2020-01-01T00:00:00Z"),
                createObjectBuilder()
                        .add("id", "https://dataspace.com/membership")
                        .add("membership", createObjectBuilder()
                                .add("status", "active")
                                .add("startTime", "2020-01-01T00:00:00Z")).build(),
                "MembershipCredential");
    }

    /**
     * Generates a Verifiable Credential (VC) for a fictional name attestation.
     *
     * @return The generated Verifiable Credential as a JSON-LD string.
     */
    static String createNameCredential() {
        return generateVc(NAME_CREDENTIAL_ISSUER,
                Instant.parse("2020-06-01T12:00:00Z"),
                createObjectBuilder()
                        .add("id", "https://some.subject/name#1")
                        .add("name", createObjectBuilder()
                                .add("name", "Test Person III.")
                                .add("shortName", "Testy")).build(),
                "NameCredential");
    }

    /**
     * Generates a Verifiable Credential (VC) for a fictional name attestation.
     *
     * @return The generated Verifiable Credential as a JSON-LD string.
     */
    static String createNameCredential(String issuer) {
        return generateVc(issuer,
                Instant.parse("2020-06-01T12:00:00Z"),
                createObjectBuilder()
                        .add("id", "https://some.subject/name#1")
                        .add("name", createObjectBuilder()
                                .add("name", "Test Person III.")
                                .add("shortName", "Testy")).build(),
                "NameCredential");
    }
}
