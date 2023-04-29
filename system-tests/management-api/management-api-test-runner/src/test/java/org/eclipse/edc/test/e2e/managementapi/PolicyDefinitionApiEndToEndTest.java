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

package org.eclipse.edc.test.e2e.managementapi;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.entity.Entity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

@EndToEndTest
public class PolicyDefinitionApiEndToEndTest extends BaseManagementApiEndToEndTest {

    @Test
    void shouldStorePolicyDefinition()  {
        var json = getResourceFileContentAsString("policy-definition.json");
        var id = baseRequest()
                .body(json)
                .contentType(JSON)
                .post()
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("id");

        assertThat(store().findById(id)).isNotNull()
                .extracting(PolicyDefinition::getPolicy).isNotNull()
                .extracting(Policy::getPermissions).asList().hasSize(1);

        baseRequest()
                .get("/" + id)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(ID, is(id))
                .body(CONTEXT, hasEntry(EDC_PREFIX, EDC_NAMESPACE));
    }

    @Test
    void shouldUpdate() {
        var json = getResourceFileContentAsString("policy-definition.json");
        var id = baseRequest()
                .body(json)
                .contentType(JSON)
                .post()
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("id");

        var createdAt = baseRequest()
                .body(Json.createObjectBuilder().build())
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(200)
                .extract().as(JsonArray.class)
                .get(0).asJsonObject()
                .getJsonNumber("edc:createdAt").longValue();

        baseRequest()
                .contentType(JSON)
                .body(json)
                .put("/" + id)
                .then()
                .statusCode(204);

        assertThat(store().findById(id))
                .extracting(Entity::getCreatedAt)
                .isNotEqualTo(createdAt);
    }

    @Test
    void shouldDelete() {
        var json = getResourceFileContentAsString("policy-definition.json");
        var id = baseRequest()
                .body(json)
                .contentType(JSON)
                .post()
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("id");

        baseRequest()
                .contentType(JSON)
                .body(json)
                .delete("/" + id)
                .then()
                .statusCode(204);

        baseRequest()
                .get("/" + id)
                .then()
                .statusCode(404);
    }

    private PolicyDefinitionStore store() {
        return controlPlane.getContext().getService(PolicyDefinitionStore.class);
    }

    private static RequestSpecification baseRequest() {
        return given()
                .port(PORT)
                .basePath("/management/v2/policydefinitions");
    }

}
