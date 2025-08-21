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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.ManagementEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.ManagementEndToEndTestContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

public class PolicyDefinitionApiV4EndToEndTest {

    abstract static class Tests {

        @Test
        void shouldStorePolicyDefinition(ManagementEndToEndTestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v4alpha/policydefinitions")
                    .then()
                    .log().ifValidationFails()
                    .contentType(JSON)
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            context.baseRequest()
                    .get("/v4alpha/policydefinitions/" + id)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is(id))
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .body("policy.permission[0].constraint[0].leftOperand", is("inForceDate"))
                    .body("policy.permission[0].constraint[0].operator", is("gteq"))
                    .body("policy.permission[0].constraint[0].rightOperand", is("contractAgreement+0s"))
                    .body("policy.prohibition[0].action", is("use"))
                    .body("policy.obligation[0].action", is("use"));
        }

        @Test
        void shouldStorePolicyDefinitionWithPrivateProperties(ManagementEndToEndTestContext context, PolicyDefinitionStore store) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .add("privateProperties", createObjectBuilder()
                            .add("newKey", "newValue")
                            .build())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v4alpha/policydefinitions")
                    .then()
                    .contentType(JSON)
                    .extract().jsonPath().getString(ID);

            var result = store.findById(id);

            assertThat(result).isNotNull()
                    .extracting(PolicyDefinition::getPolicy).isNotNull()
                    .extracting(Policy::getPermissions).asList().hasSize(1);
            Map<String, Object> privateProp = new HashMap<>();
            privateProp.put("https://w3id.org/edc/v0.0.1/ns/newKey", "newValue");
            assertThat(result).isNotNull()
                    .extracting(PolicyDefinition::getPrivateProperties).isEqualTo(privateProp);

            context.baseRequest()
                    .get("/v4alpha/policydefinitions/" + id)
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is(id))
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .log().all()
                    .body("policy.permission[0].constraint[0].operator", is("gteq"));
        }

        @Test
        void queryPolicyDefinitionWithSimplePrivateProperties(ManagementEndToEndTestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .add("privateProperties", createObjectBuilder()
                            .add("newKey", createObjectBuilder().add(ID, "newValue"))
                            .build())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v4alpha/policydefinitions")
                    .then()
                    .contentType(JSON)
                    .extract().jsonPath().getString(ID);

            var matchingQuery = context.queryV2(
                    criterion("id", "=", id),
                    criterion("privateProperties.'https://w3id.org/edc/v0.0.1/ns/newKey'.@id", "=", "newValue")
            );

            context.baseRequest()
                    .body(matchingQuery)
                    .contentType(JSON)
                    .post("/v4alpha/policydefinitions/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1));


            var nonMatchingQuery = context.queryV2(
                    criterion("id", "=", id),
                    criterion("privateProperties.'https://w3id.org/edc/v0.0.1/ns/newKey'.@id", "=", "somethingElse")
            );

            context.baseRequest()
                    .body(nonMatchingQuery)
                    .contentType(JSON)
                    .post("/v4alpha/policydefinitions/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(0));
        }

        @Test
        void shouldUpdate(ManagementEndToEndTestContext context, PolicyDefinitionStore store) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v4alpha/policydefinitions")
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            context.baseRequest()
                    .contentType(JSON)
                    .get("/v4alpha/policydefinitions/" + id)
                    .then()
                    .statusCode(200);

            context.baseRequest()
                    .contentType(JSON)
                    .body(createObjectBuilder(requestBody)
                            .add(ID, id)
                            .add("privateProperties", createObjectBuilder().add("privateProperty", "value"))
                            .build())
                    .put("/v4alpha/policydefinitions/" + id)
                    .then()
                    .statusCode(204);

            assertThat(store.findById(id))
                    .extracting(PolicyDefinition::getPrivateProperties)
                    .asInstanceOf(MAP)
                    .isNotEmpty();
        }

        @Test
        void shouldDelete(ManagementEndToEndTestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v4alpha/policydefinitions")
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            context.baseRequest()
                    .delete("/v4alpha/policydefinitions/" + id)
                    .then()
                    .statusCode(204);

            context.baseRequest()
                    .get("/v4alpha/policydefinitions/" + id)
                    .then()
                    .statusCode(404);
        }

        @Test
        void shouldDeleteWithProperties(ManagementEndToEndTestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .add("privateProperties", createObjectBuilder()
                            .add("newKey", "newValue")
                            .build())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v4alpha/policydefinitions")
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            context.baseRequest()
                    .delete("/v4alpha/policydefinitions/" + id)
                    .then()
                    .statusCode(204);

            context.baseRequest()
                    .get("/v4alpha/policydefinitions/" + id)
                    .then()
                    .statusCode(404);
        }

        @Test
        void shouldValidatePolicy(ManagementEndToEndTestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleInvalidOdrlPolicy())
                    .build();

            context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v4alpha/policydefinitions")
                    .then()
                    .statusCode(400)
                    .body("size()", is(2))
                    .body("[0].message", startsWith("leftOperand 'https://w3id.org/edc/v0.0.1/ns/left' is not bound to any scopes"))
                    .body("[1].message", startsWith("leftOperand 'https://w3id.org/edc/v0.0.1/ns/left' is not bound to any functions"));
        }

        @Test
        void shouldCreateEvaluationPlan(ManagementEndToEndTestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v4alpha/policydefinitions")
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            var planBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyEvaluationPlanRequest")
                    .add("policyScope", "catalog")
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(planBody)
                    .post("/v4alpha/policydefinitions/" + id + "/evaluationplan")
                    .then()
                    .statusCode(200)
                    .body("preValidators.size()", is(0))
                    .body("permissionSteps[0].isFiltered", is(false))
                    .body("permissionSteps[0].filteringReasons.size()", is(0))
                    .body("permissionSteps[0].constraintSteps[0].'@type'", is("AtomicConstraintStep"))
                    .body("permissionSteps[0].constraintSteps[0].isFiltered", is(true))
                    .body("permissionSteps[0].constraintSteps[0].filteringReasons.size()", is(2))
                    .body("permissionSteps[0].constraintSteps[0].functionName", nullValue())
                    .body("permissionSteps[0].constraintSteps[0].functionParams.size()", is(3))
                    .body("prohibitionSteps[0].isFiltered", is(false))
                    .body("prohibitionSteps[0].filteringReasons", notNullValue())
                    .body("prohibitionSteps[0].constraintSteps.size()", is(0))
                    .body("obligationSteps[0].isFiltered", is(false))
                    .body("obligationSteps[0].filteringReasons.size()", is(0))
                    .body("obligationSteps[0].constraintSteps.size()", is(0))
                    .body("postValidators.size()", is(0));

        }

        @Test
        void shouldNotUpdatePolicyDefinition_whenValidationFails(ManagementEndToEndTestContext context) {
            var validRequestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", emptyOdrlPolicy())
                    .build();

            var id = context.baseRequest()
                    .body(validRequestBody)
                    .contentType(JSON)
                    .post("/v4alpha/policydefinitions")
                    .then()
                    .contentType(JSON)
                    .extract().jsonPath().getString(ID);

            context.baseRequest()
                    .contentType(JSON)
                    .get("/v4alpha/policydefinitions/" + id)
                    .then()
                    .statusCode(200);

            var inValidRequestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleInvalidOdrlPolicy())
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(inValidRequestBody)
                    .put("/v4alpha/policydefinitions/" + id)
                    .then()
                    .statusCode(400)
                    .contentType(JSON)
                    .body("size()", is(2))
                    .body("[0].message", startsWith("leftOperand 'https://w3id.org/edc/v0.0.1/ns/left' is not bound to any scopes"))
                    .body("[1].message", startsWith("leftOperand 'https://w3id.org/edc/v0.0.1/ns/left' is not bound to any functions"));
        }

        private JsonObject emptyOdrlPolicy() {
            return createObjectBuilder()
                    .add(TYPE, "Set")
                    .build();
        }

        private JsonObject sampleInvalidOdrlPolicy() {
            return createObjectBuilder()
                    .add(TYPE, "Set")
                    .add("permission", createArrayBuilder()
                            .add(createObjectBuilder()
                                    .add("action", "use")
                                    .add("constraint", createArrayBuilder().add(createObjectBuilder()
                                                    .add("leftOperand", "https://w3id.org/edc/v0.0.1/ns/left")
                                                    .add("operator", "eq")
                                                    .add("rightOperand", "value"))
                                            .build()))
                            .build())
                    .build();
        }

        private JsonObject sampleOdrlPolicy() {
            return createObjectBuilder()
                    .add(TYPE, "Set")
                    .add("permission", createArrayBuilder()
                            .add(createObjectBuilder()
                                    .add("action", "use")
                                    .add("constraint", createArrayBuilder().add(createObjectBuilder()
                                                    .add("leftOperand", "inForceDate")
                                                    .add("operator", "gteq")
                                                    .add("rightOperand", "contractAgreement+0s"))
                                            .build()))
                            .build())
                    .add("prohibition", createArrayBuilder()
                            .add(createObjectBuilder()
                                    .add("action", "use")
                            ))
                    .add("obligation", createArrayBuilder()
                            .add(createObjectBuilder()
                                    .add("action", "use")
                            )
                    )
                    .build();
        }

        private JsonArray jsonLdContext() {
            return createArrayBuilder()
                    .add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                    .build();
        }

    }

    @Nested
    @EndToEndTest
    @ExtendWith(ManagementEndToEndExtension.InMemory.class)
    class InMemory extends Tests {

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        @Order(0)
        static PostgresqlEndToEndExtension postgres = new PostgresqlEndToEndExtension();

        @RegisterExtension
        static ManagementEndToEndExtension runtime = new ManagementEndToEndExtension.Postgres(postgres);

    }

}
