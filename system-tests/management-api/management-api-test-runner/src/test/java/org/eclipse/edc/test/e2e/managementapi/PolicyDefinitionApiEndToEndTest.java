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

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class PolicyDefinitionApiEndToEndTest {

    abstract static class Tests {

        @Test
        void shouldStorePolicyDefinition(ManagementEndToEndTestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder()
                            .add(VOCAB, EDC_NAMESPACE)
                            .build())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v3/policydefinitions")
                    .then()
                    .contentType(JSON)
                    .extract().jsonPath().getString(ID);

            context.baseRequest()
                    .get("/v3/policydefinitions/" + id)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is(id))
                    .body(CONTEXT, hasEntry(EDC_PREFIX, EDC_NAMESPACE))
                    .body(CONTEXT, hasEntry(ODRL_PREFIX, ODRL_SCHEMA))
                    .body("policy.'odrl:permission'.'odrl:constraint'.'odrl:operator'.@id", is("odrl:eq"))
                    .body("policy.'odrl:prohibition'.'odrl:remedy'.'odrl:action'.@id", is("odrl:anonymize"))
                    .body("policy.'odrl:obligation'.'odrl:consequence'.size()", is(2));
        }

        @Test
        void shouldStorePolicyDefinitionWithPrivateProperties(ManagementEndToEndTestContext context, PolicyDefinitionStore store) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder()
                            .add(VOCAB, EDC_NAMESPACE)
                            .build())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .add("privateProperties", createObjectBuilder()
                            .add("newKey", "newValue")
                            .build())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v3/policydefinitions")
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
                    .get("/v3/policydefinitions/" + id)
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
        void queryPolicyDefinitionWithSimplePrivateProperties(ManagementEndToEndTestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder()
                            .add(VOCAB, EDC_NAMESPACE)
                            .build())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .add("privateProperties", createObjectBuilder()
                            .add("newKey", createObjectBuilder().add(ID, "newValue"))
                            .build())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v3/policydefinitions")
                    .then()
                    .contentType(JSON)
                    .extract().jsonPath().getString(ID);

            var matchingQuery = context.query(
                    criterion("id", "=", id),
                    criterion("privateProperties.'https://w3id.org/edc/v0.0.1/ns/newKey'.@id", "=", "newValue")
            );

            context.baseRequest()
                    .body(matchingQuery)
                    .contentType(JSON)
                    .post("/v3/policydefinitions/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1));


            var nonMatchingQuery = context.query(
                    criterion("id", "=", id),
                    criterion("privateProperties.'https://w3id.org/edc/v0.0.1/ns/newKey'.@id", "=", "somethingElse")
            );

            context.baseRequest()
                    .body(nonMatchingQuery)
                    .contentType(JSON)
                    .post("/v3/policydefinitions/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(0));
        }

        @Test
        void shouldUpdate(ManagementEndToEndTestContext context, PolicyDefinitionStore store) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder()
                            .add(VOCAB, EDC_NAMESPACE)
                            .build())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v3/policydefinitions")
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            context.baseRequest()
                    .contentType(JSON)
                    .get("/v3/policydefinitions/" + id)
                    .then()
                    .statusCode(200);

            context.baseRequest()
                    .contentType(JSON)
                    .body(createObjectBuilder(requestBody)
                            .add(ID, id)
                            .add("privateProperties", createObjectBuilder().add("privateProperty", "value"))
                            .build())
                    .put("/v3/policydefinitions/" + id)
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
                    .add(CONTEXT, createObjectBuilder()
                            .add(VOCAB, EDC_NAMESPACE)
                            .build())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v3/policydefinitions")
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            context.baseRequest()
                    .delete("/v3/policydefinitions/" + id)
                    .then()
                    .statusCode(204);

            context.baseRequest()
                    .get("/v3/policydefinitions/" + id)
                    .then()
                    .statusCode(404);
        }

        @Test
        void shouldDeleteWithProperties(ManagementEndToEndTestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder()
                            .add(VOCAB, EDC_NAMESPACE)
                            .build())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .add("privateProperties", createObjectBuilder()
                            .add("newKey", "newValue")
                            .build())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v3/policydefinitions")
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            context.baseRequest()
                    .delete("/v3/policydefinitions/" + id)
                    .then()
                    .statusCode(204);

            context.baseRequest()
                    .get("/v3/policydefinitions/" + id)
                    .then()
                    .statusCode(404);
        }

        @Test
        void shouldValidatePolicy(ManagementEndToEndTestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder()
                            .add(VOCAB, EDC_NAMESPACE)
                            .build())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v3/policydefinitions")
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            context.baseRequest()
                    .contentType(JSON)
                    .post("/v3.1alpha/policydefinitions/" + id + "/validate")
                    .then()
                    .statusCode(200)
                    .body("isValid", is(false))
                    .body("errors.size()", is(3));

        }

        @Test
        void shouldCreateEvaluationPlan(ManagementEndToEndTestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder()
                            .add(VOCAB, EDC_NAMESPACE)
                            .build())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .build();

            var id = context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v3/policydefinitions")
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            var planBody = createObjectBuilder().add(CONTEXT, createObjectBuilder()
                            .add(VOCAB, EDC_NAMESPACE)
                            .build())
                    .add(TYPE, "PolicyEvaluationPlanRequest")
                    .add("policyScope", "catalog")
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(planBody)
                    .post("/v3.1alpha/policydefinitions/" + id + "/evaluationplan")
                    .then()
                    .statusCode(200)
                    .body("preValidators.size()", is(0))
                    .body("permissionSteps.isFiltered", is(false))
                    .body("permissionSteps.filteringReasons.size()", is(0))
                    .body("permissionSteps.constraintSteps.'@type'", is("AtomicConstraintStep"))
                    .body("permissionSteps.constraintSteps.isFiltered", is(true))
                    .body("permissionSteps.constraintSteps.filteringReasons.size()", is(2))
                    .body("permissionSteps.constraintSteps.functionName", nullValue())
                    .body("permissionSteps.constraintSteps.functionParams.size()", is(3))
                    .body("prohibitionSteps.isFiltered", is(true))
                    .body("prohibitionSteps.filteringReasons", notNullValue())
                    .body("prohibitionSteps.constraintSteps.size()", is(0))
                    .body("obligationSteps.isFiltered", is(false))
                    .body("obligationSteps.filteringReasons.size()", is(0))
                    .body("obligationSteps.constraintSteps.size()", is(0))
                    .body("postValidators.size()", is(0));

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
                    .add("prohibition", createArrayBuilder()
                            .add(createObjectBuilder()
                                    .add("target", "http://example.com/data:77")
                                    .add("action", "index")
                                    .add("remedy", createObjectBuilder()
                                            .add("action", "anonymize"))
                            ))
                    .add("obligation", createArrayBuilder()
                            .add(createObjectBuilder()
                                    .add("target", "http://example.com/data:77")
                                    .add("action", "use")
                                    .add("consequence", createArrayBuilder()
                                            .add(createObjectBuilder().add("action", "use"))
                                            .add(createObjectBuilder().add("action", "anonymize"))
                                    )
                            )
                    )
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
    @ExtendWith(ManagementEndToEndExtension.Postgres.class)
    class Postgres extends Tests {
    }

}
