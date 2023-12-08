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

import io.restassured.http.ContentType;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.entity.Entity;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

@EndToEndTest
public class PolicyDefinitionApiEndToEndTest extends BaseManagementApiEndToEndTest {


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
                .post("/v2/policydefinitions")
                .then()
                .contentType(JSON)
                .extract().jsonPath().getString(ID);

        assertThat(store().findById(id)).isNotNull()
                .extracting(PolicyDefinition::getPolicy).isNotNull()
                .extracting(Policy::getPermissions).asList().hasSize(1);

        baseRequest()
                .get("/v2/policydefinitions/" + id)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(ID, is(id))
                .body(CONTEXT, hasEntry(EDC_PREFIX, EDC_NAMESPACE))
                .body(CONTEXT, hasEntry(ODRL_PREFIX, ODRL_SCHEMA))
                .log().all()
                .body("policy.'odrl:permission'.'odrl:constraint'.'odrl:operator'.@id", is("odrl:eq"));
    }

    @Test
    void shouldStorePolicyDefinitionWithPrivateProperties() {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder()
                        .add("edc", EDC_NAMESPACE)
                        .build())
                .add(TYPE, "PolicyDefinition")
                .add("policy", sampleOdrlPolicy())
                .add("privateProperties", createObjectBuilder()
                        .add("newKey", "newValue")
                        .build())
                .build();

        var id = baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post("/v2/policydefinitions")
                .then()
                .contentType(JSON)
                .extract().jsonPath().getString(ID);

        PolicyDefinition result = store().findById(id);
        assertThat(result).isNotNull()
                .extracting(PolicyDefinition::getPolicy).isNotNull()
                .extracting(Policy::getPermissions).asList().hasSize(1);
        Map<String, Object> privateProp = new HashMap<>();
        privateProp.put("https://w3id.org/edc/v0.0.1/ns/newKey", "newValue");
        assertThat(result).isNotNull()
                .extracting(PolicyDefinition::getPrivateProperties).isEqualTo(privateProp);

        baseRequest()
                .get("/v2/policydefinitions/" + id)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(ID, is(id))
                .body(CONTEXT, hasEntry(EDC_PREFIX, EDC_NAMESPACE))
                .body(CONTEXT, hasEntry(ODRL_PREFIX, ODRL_SCHEMA))
                .log().all()
                .body("policy.'odrl:permission'.'odrl:constraint'.'odrl:operator'.@id", is("odrl:eq"));
    }

    @Test
    void queryPolicyDefinitionWithSimplePrivateProperties() {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder()
                        .add("edc", EDC_NAMESPACE)
                        .build())
                .add(TYPE, "PolicyDefinition")
                .add("policy", sampleOdrlPolicy())
                .add("edc:privateProperties", createObjectBuilder()
                        .add("newKey", "newValue")
                        .build())
                .build();

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post("/v2/policydefinitions")
                .then()
                .contentType(JSON)
                .extract().jsonPath().getString(ID);


        var query = createSingleFilterQuery(
                "https://w3id.org/edc/v0.0.1/ns/privateProperties.https://w3id.org/edc/v0.0.1/ns/newKey",
                "=",
                "newValue");


        var resp = baseRequest()
                .contentType(ContentType.JSON)
                .body(query)
                .post("/v2/policydefinitions/request");
        assertThat(resp.getBody().toString()).isEqualTo("");
    }

    private JsonObject createSingleFilterQuery(String leftOperand, String operator, String rightOperand) {
        var criteria = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(TYPE, "Criterion")
                        .add("operandLeft", leftOperand)
                        .add("operator", operator)
                        .add("operandRight", rightOperand)
                );

        return createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "QuerySpec")
                .add("filterExpression", criteria)
                .build();
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
                .post("/v2/policydefinitions")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString(ID);

        var createdAt = baseRequest()
                .contentType(JSON)
                .post("/v2/policydefinitions/request")
                .then()
                .statusCode(200)
                .extract().as(JsonArray.class)
                .get(0).asJsonObject()
                .getJsonNumber("createdAt").longValue();

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder(requestBody).add(ID, id).build())
                .put("/v2/policydefinitions/" + id)
                .then()
                .statusCode(204);

        assertThat(store().findById(id))
                .extracting(Entity::getCreatedAt)
                .isNotEqualTo(createdAt);
    }

    @Test
    void shouldUpdateWithProperties() {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder()
                        .add("edc", EDC_NAMESPACE)
                        .build())
                .add(TYPE, "PolicyDefinition")
                .add("policy", sampleOdrlPolicy())
                .add("privateProperties", createObjectBuilder()
                        .add("newKey", "newValue")
                        .build())
                .build();

        var updatedBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder()
                        .add("edc", EDC_NAMESPACE)
                        .build())
                .add(TYPE, "PolicyDefinition")
                .add("policy", sampleOdrlPolicy())
                .add("privateProperties", createObjectBuilder()
                        .add("newKey", "updatedValue")
                        .build())
                .build();

        var id = baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post("/v2/policydefinitions")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString(ID);

        var createdAt = baseRequest()
                .contentType(JSON)
                .post("/v2/policydefinitions/request")
                .then()
                .statusCode(200)
                .extract().as(JsonArray.class)
                .get(0).asJsonObject()
                .getJsonNumber("createdAt").longValue();

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder(updatedBody).add(ID, id).build())
                .put("/v2/policydefinitions/" + id)
                .then()
                .statusCode(204);

        var result = store().findById(id);
        assertThat(result)
                .extracting(Entity::getCreatedAt)
                .isNotEqualTo(createdAt);


        Map<String, Object> privateProp = new HashMap<>();
        privateProp.put("https://w3id.org/edc/v0.0.1/ns/newKey", "updatedValue");
        assertThat(result).isNotNull()
                .extracting(PolicyDefinition::getPrivateProperties).isEqualTo(privateProp);

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
                .post("/v2/policydefinitions")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString(ID);

        baseRequest()
                .delete("/v2/policydefinitions/" + id)
                .then()
                .statusCode(204);

        baseRequest()
                .get("/v2/policydefinitions/" + id)
                .then()
                .statusCode(404);
    }


    @Test
    void shouldDeleteWithProperties() {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder()
                        .add("edc", EDC_NAMESPACE)
                        .build())
                .add(TYPE, "PolicyDefinition")
                .add("policy", sampleOdrlPolicy())
                .add("privateProperties", createObjectBuilder()
                        .add("newKey", "newValue")
                        .build())
                .build();

        var id = baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post("/v2/policydefinitions")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString(ID);

        baseRequest()
                .delete("/v2/policydefinitions/" + id)
                .then()
                .statusCode(204);

        baseRequest()
                .get("/v2/policydefinitions/" + id)
                .then()
                .statusCode(404);
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
