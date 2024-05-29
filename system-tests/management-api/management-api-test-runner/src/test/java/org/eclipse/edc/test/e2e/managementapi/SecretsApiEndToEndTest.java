/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.managementapi;

import io.restassured.http.ContentType;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_VALUE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Secret V1 endpoints end-to-end tests
 */
public class SecretsApiEndToEndTest {

    abstract static class Tests {

        @Test
        void getSecretById(ManagementEndToEndTestContext context, Vault vault) {
            var id = UUID.randomUUID().toString();
            var value = "secret-value";
            vault.storeSecret(id, value);

            context.baseRequest()
                    .get("/v3/secrets/" + id)
                    .then()
                    .statusCode(200)
                    .body(notNullValue())
                    .body(CONTEXT, hasEntry(EDC_PREFIX, EDC_NAMESPACE))
                    .body(ID, equalTo(id))
                    .body("value", equalTo(value));
        }

        @Test
        void createSecret_shouldBeStored(ManagementEndToEndTestContext context, Vault vault) {
            var id = UUID.randomUUID().toString();
            var value = "secret-value";
            var secretJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, EDC_SECRET_TYPE)
                    .add(ID, id)
                    .add(EDC_SECRET_VALUE, value)
                    .build();

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(secretJson)
                    .post("/v3/secrets")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(id));

            assertThat(vault.resolveSecret(id))
                    .isNotNull()
                    .isEqualTo(value);
        }

        @Test
        void createSecret_shouldFail_whenBodyIsNotValid(ManagementEndToEndTestContext context) {
            var secretJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, EDC_SECRET_TYPE)
                    .add(ID, " ")
                    .add(EDC_SECRET_VALUE, "secret-value")
                    .build();

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(secretJson)
                    .post("/v3/secrets")
                    .then()
                    .log().ifError()
                    .statusCode(400);
        }

        @Test
        void updateSecret(ManagementEndToEndTestContext context, Vault vault) {
            var id = UUID.randomUUID().toString();
            var newValue = "new-value";
            vault.storeSecret(id, "secret-value");

            var secretJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, EDC_SECRET_TYPE)
                    .add(ID, id)
                    .add(EDC_SECRET_VALUE, newValue)
                    .build();

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(secretJson)
                    .put("/v3/secrets")
                    .then()
                    .log().all()
                    .statusCode(204)
                    .body(notNullValue());

            var vaultSecret = vault.resolveSecret(id);
            assertThat(vaultSecret).isNotNull().isEqualTo(newValue);
        }

    }

    @Nested
    @EndToEndTest
    @ExtendWith(ManagementEndToEndExtension.InMemory.class)
    class InMemory extends Tests { }
}
