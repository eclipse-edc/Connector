/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.iam.verifiablecredentials.rules;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSchema;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResource;

class HasValidSubjectSchemaTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HasValidSubjectSchema rule = new HasValidSubjectSchema(mapper);

    @Test
    void validate_oneSchema_oneSubject() {
        var cred = credential()
                .credentialSubject(subject().build())
                .credentialSchema(new CredentialSchema(getResource("personSchema.json").toString(), "JsonSchemaValidator2018"))
                .build();

        assertThat(rule.apply(cred)).isSucceeded();
    }

    @Test
    void validate_oneSchema_multipleSubjects_allValid() {
        var cred = credential()
                .credentialSubject(subject().build())
                .credentialSubject(subject()
                        .claim("anotherClaim", "anotherValue")
                        .build())
                .credentialSchema(new CredentialSchema(getResource("personSchema.json").toString(), "JsonSchemaValidator2018"))
                .build();

        assertThat(rule.apply(cred)).isSucceeded();
    }

    @Test
    void validate_oneSchema_multipleSubjects_oneValidates() {
        var cred = credential()
                .credentialSubject(subject().build())
                .credentialSubject(CredentialSubject.Builder.newInstance().id(UUID.randomUUID().toString())
                        .claim("anotherClaim", "anotherValue")
                        .build())
                .credentialSchema(new CredentialSchema(getResource("personSchema.json").toString(), "JsonSchemaValidator2018"))
                .build();

        assertThat(rule.apply(cred)).isFailed();
    }

    @Test
    void validate_oneSubjectMultipleSchemas_nonIntersect() {
        var cred = credential()
                .credentialSubject(subject()
                        .claim("companyName", "QuizzQuazz Industries Inc.")
                        .claim("email", "info@quizzquazz.com")
                        .claim("street", "FooBar Street 15")
                        .claim("city", "BarBazTown")
                        .claim("postalCode", 12345)
                        .build())
                .credentialSchema(new CredentialSchema(getResource("personSchema.json").toString(), "JsonSchemaValidator2018"))
                .credentialSchema(new CredentialSchema(getResource("companyAddressSchema.json").toString(), "JsonSchemaValidator2018"))
                .build();

        assertThat(rule.apply(cred)).isSucceeded();
    }

    @Test
    void validate_oneSubjectMultipleSchemas_intersectWithConflict() {
        var cred = credential()
                .credentialSubject(subject()
                        .claim("companyName", "QuizzQuazz Industries Inc.")
                        .claim("email", "info@quizzquazz.com")
                        .claim("street", "FooBar Street 15")
                        .claim("city", "BarBazTown")
                        .claim("postalCode", 12345)
                        .build())
                .credentialSchema(new CredentialSchema(getResource("companyAddressSchema.json").toString(), "JsonSchemaValidator2018"))
                .credentialSchema(new CredentialSchema(getResource("personAddressSchema.json").toString(), "JsonSchemaValidator2018"))
                .build();

        // personAddressSchema defines postalCode as string, companyAddressSchema defines it as int -> conflict
        assertThat(rule.apply(cred)).isFailed();
    }

    @Test
    void validate_multipleSchemas_oneSubject_allValid() {
        var cred = credential()
                .credentialSubject(subject().build())
                .credentialSchema(new CredentialSchema(getResource("personSchema.json").toString(), "JsonSchemaValidator2018"))
                .credentialSchema(new CredentialSchema(getResource("genericNameSchema.json").toString(), "JsonSchemaValidator2019"))
                .build();

        assertThat(rule.apply(cred)).isSucceeded();
    }

    @Test
    void validate_multipleSubjects_oneViolatesRestrictiveSchema() {
        var cred = credential()
                .credentialSubject(subject().build()) // OK
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .claim("name", "foo bar") // satisfies name schema, but violates person schema
                        .build())
                .credentialSchema(new CredentialSchema(getResource("personSchema.json").toString(), "JsonSchemaValidator2018"))
                .credentialSchema(new CredentialSchema(getResource("genericNameSchema.json").toString(), "JsonSchemaValidator2019"))
                .build();

        assertThat(rule.apply(cred)).isFailed();
    }

    @Test
    void validate_noSchema() {
        var cred = credential()
                .credentialSubject(subject().build())
                .build();

        assertThat(rule.apply(cred)).isSucceeded();

        var cred2 = credential()
                .credentialSubject(subject().build())
                .credentialSchemas(null)
                .build();

        assertThat(rule.apply(cred2)).isSucceeded();

        var cred3 = credential()
                .credentialSubject(subject().build())
                .credentialSchema(null)
                .build();

        assertThat(rule.apply(cred3)).isSucceeded();
    }

    @Test
    void validate_oneSubjectViolates() {
        var cred = credential()
                .credentialSubject(subject()
                        .claim("name", 14)
                        .build()) // name should be a string
                .credentialSchema(new CredentialSchema(getResource("personSchema.json").toString(), "JsonSchemaValidator2018"))
                .build();

        var result = rule.apply(cred);
        assertThat(result).isFailed();
        assertThat(result.getFailureMessages()).hasSize(1);
    }

    @Test
    void validate_claimNotCoveredBySchema_shouldSucceed() {
        var cred = credential()
                .credentialSubject(subject()
                        .claim("another-property", 14)
                        .build())
                .credentialSchema(new CredentialSchema(getResource("personSchema.json").toString(), "JsonSchemaValidator2018"))
                .build();

        var result = rule.apply(cred);
        assertThat(result).isSucceeded();
    }


    private VerifiableCredential.Builder credential() {
        return VerifiableCredential.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .issuanceDate(Instant.now())
                .issuer(new Issuer(UUID.randomUUID().toString()))
                .type("VerifiableCredential");
    }

    private CredentialSubject.Builder subject() {
        return CredentialSubject.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .claim("type", "PersonSubject")
                .claim("name", "Alice Smith")
                .claim("birthDate", "2001-12-02T00:00:00Z");
    }
}