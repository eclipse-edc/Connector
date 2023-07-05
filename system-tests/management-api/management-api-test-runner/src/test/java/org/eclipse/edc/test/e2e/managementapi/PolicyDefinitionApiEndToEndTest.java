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
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.entity.Entity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

@EndToEndTest
public class PolicyDefinitionApiEndToEndTest extends BaseManagementApiEndToEndTest {

    private static final String BASE_PATH = "/management/v2/policydefinitions";

    @Test
    void shouldStorePolicyDefinition() {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder()
                        .add("edc", EDC_NAMESPACE)
                        .build())
                .add(TYPE, "PolicyDefinition")
                .add("policy", sampleOdrlPolicy())
                .build();

        var id = baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post()
                .then()
                .contentType(JSON)
                .extract().jsonPath().getString(ID);

        assertThat(store().findById(id)).isNotNull()
                .extracting(PolicyDefinition::getPolicy).isNotNull()
                .extracting(Policy::getPermissions).asList().hasSize(1);

        baseRequest()
                .get("/" + id)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(ID, is(id))
                .body(CONTEXT, hasEntry(EDC_PREFIX, EDC_NAMESPACE))
                .log().all()
                .body("'edc:policy'.'odrl:permission'.'odrl:constraint'.'odrl:operator'.@id", is("odrl:eq"));
    }

    @Test
    void shouldUpdate() {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder()
                        .add("edc", EDC_NAMESPACE)
                        .build())
                .add(TYPE, "PolicyDefinition")
                .add("policy", sampleOdrlPolicy())
                .build();

        var id = baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post()
                .then()
                .statusCode(200)
                .extract().jsonPath().getString(ID);

        var createdAt = baseRequest()
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(200)
                .extract().as(JsonArray.class)
                .get(0).asJsonObject()
                .getJsonNumber("edc:createdAt").longValue();

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder(requestBody).add(ID, id).build())
                .put("/" + id)
                .then()
                .statusCode(204);

        assertThat(store().findById(id))
                .extracting(Entity::getCreatedAt)
                .isNotEqualTo(createdAt);
    }

    @Test
    void shouldDelete() {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder()
                        .add("edc", EDC_NAMESPACE)
                        .build())
                .add(TYPE, "PolicyDefinition")
                .add("policy", sampleOdrlPolicy())
                .build();


        var id = baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post()
                .then()
                .statusCode(200)
                .extract().jsonPath().getString(ID);

        baseRequest()
                .delete("/" + id)
                .then()
                .statusCode(204);

        baseRequest()
                .get("/" + id)
                .then()
                .statusCode(404);
    }

    private RequestSpecification baseRequest() {
        return given()
                .port(PORT)
                .basePath(BASE_PATH);
    }

    private JsonObject sampleOdrlPolicy() {
        return createObjectBuilder()
                .add(CONTEXT, "http://www.w3.org/ns/odrl.jsonld")
                .add(TYPE, "Set")
                .add("permission", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("target", "http://example.com/asset:9898.movie")
                                .add("action", "use")
                                .add("constraint", createObjectBuilder()
                                                .add("leftOperand", "left")
                                                .add("operator", "eq")
                                                .add("rightOperand", "value"))
                                .build())
                        .build())
                .build();
    }

    private PolicyDefinitionStore store() {
        return controlPlane.getContext().getService(PolicyDefinitionStore.class);
    }

}
