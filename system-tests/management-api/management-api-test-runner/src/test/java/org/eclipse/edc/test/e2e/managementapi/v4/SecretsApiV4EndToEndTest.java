/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.managementapi.v4;

import io.restassured.http.ContentType;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.test.e2e.managementapi.ManagementEndToEndTestContext;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.UUID;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE_TERM;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class SecretsApiV4EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        @Test
        void getSecretById(ManagementEndToEndTestContext context, Vault vault) {
            var id = UUID.randomUUID().toString();
            var value = "secret-value";
            vault.storeSecret(id, value);

            context.baseRequest()
                    .get("/v4beta/secrets/" + id)
                    .then()
                    .statusCode(200)
                    .body(notNullValue())
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .body(ID, equalTo(id))
                    .body("value", equalTo(value));
        }

        @Test
        void createSecret_shouldBeStored(ManagementEndToEndTestContext context, Vault vault) {
            var id = UUID.randomUUID().toString();
            var value = "secret-value";
            var secretJson = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, EDC_SECRET_TYPE_TERM)
                    .add(ID, id)
                    .add("value", value)
                    .build();

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(secretJson)
                    .post("/v4beta/secrets")
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
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, EDC_SECRET_TYPE_TERM)
                    .add(ID, " ")
                    .add("value", "secret-value")
                    .build();

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(secretJson)
                    .post("/v4beta/secrets")
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
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, EDC_SECRET_TYPE_TERM)
                    .add(ID, id)
                    .add("value", newValue)
                    .build();

            context.baseRequest()
                    .contentType(ContentType.JSON)
                    .body(secretJson)
                    .put("/v4beta/secrets")
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
    class InMemory extends Tests {

        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .paramProvider(ManagementEndToEndTestContext.class, ManagementEndToEndTestContext::forContext)
                .build();
    }
}
