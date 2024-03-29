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
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.createDatabase;
import static org.eclipse.edc.test.e2e.managementapi.Runtimes.inMemoryRuntime;
import static org.eclipse.edc.test.e2e.managementapi.Runtimes.postgresRuntime;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

public class PolicyDefinitionApiEndToEndTest {

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        public static final EdcRuntimeExtension RUNTIME = inMemoryRuntime();

        InMemory() {
            super(RUNTIME);
        }

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        static final BeforeAllCallback CREATE_DATABASE = context -> createDatabase("runtime");

        @RegisterExtension
        public static final EdcRuntimeExtension RUNTIME = postgresRuntime();

        Postgres() {
            super(RUNTIME);
        }
    }

    abstract static class Tests extends ManagementApiEndToEndTestBase {

        Tests(EdcRuntimeExtension runtime) {
            super(runtime);
        }

        @Test
        void shouldStorePolicyDefinition() {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder()
                            .add(VOCAB, EDC_NAMESPACE)
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
                    .extracting(PolicyDefinition::getPolicy).isNotNull().satisfies(policy -> {
                        assertThat(policy.getPermissions()).hasSize(1);
                        assertThat(policy.getProhibitions()).hasSize(1).first()
                                .extracting(Prohibition::getRemedies).asInstanceOf(list(Duty.class)).first()
                                .extracting(Rule::getAction).extracting(Action::getType).isEqualTo(ODRL_SCHEMA + "anonymize");
                    });

            baseRequest()
                    .get("/v2/policydefinitions/" + id)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is(id))
                    .body(CONTEXT, hasEntry(EDC_PREFIX, EDC_NAMESPACE))
                    .body(CONTEXT, hasEntry(ODRL_PREFIX, ODRL_SCHEMA))
                    .body("policy.'odrl:permission'.'odrl:constraint'.'odrl:operator'.@id", is("odrl:eq"))
                    .body("policy.'odrl:prohibition'.'odrl:remedy'.'odrl:action'.'odrl:type'", is(ODRL_SCHEMA + "anonymize"));
        }

        @Test
        void shouldStorePolicyDefinitionWithPrivateProperties() {
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

            var id = baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v2/policydefinitions")
                    .then()
                    .contentType(JSON)
                    .extract().jsonPath().getString(ID);

            var result = store().findById(id);

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
                            .add(VOCAB, EDC_NAMESPACE)
                            .build())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .add("privateProperties", createObjectBuilder()
                            .add("newKey", createObjectBuilder().add(ID, "newValue"))
                            .build())
                    .build();

            var id = baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v2/policydefinitions")
                    .then()
                    .contentType(JSON)
                    .extract().jsonPath().getString(ID);

            var matchingQuery = query(
                    criterion("id", "=", id),
                    criterion("privateProperties.'https://w3id.org/edc/v0.0.1/ns/newKey'.@id", "=", "newValue")
            );

            baseRequest()
                    .body(matchingQuery)
                    .contentType(JSON)
                    .post("/v2/policydefinitions/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1));


            var nonMatchingQuery = query(
                    criterion("id", "=", id),
                    criterion("privateProperties.'https://w3id.org/edc/v0.0.1/ns/newKey'.@id", "=", "somethingElse")
            );

            baseRequest()
                    .body(nonMatchingQuery)
                    .contentType(JSON)
                    .post("/v2/policydefinitions/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(0));
        }

        @Test
        void shouldUpdate() {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder()
                            .add(VOCAB, EDC_NAMESPACE)
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
                    .contentType(JSON)
                    .get("/v2/policydefinitions/" + id)
                    .then()
                    .statusCode(200);

            baseRequest()
                    .contentType(JSON)
                    .body(createObjectBuilder(requestBody)
                            .add(ID, id)
                            .add("privateProperties", createObjectBuilder().add("privateProperty", "value"))
                            .build())
                    .put("/v2/policydefinitions/" + id)
                    .then()
                    .statusCode(204);

            assertThat(store().findById(id))
                    .extracting(PolicyDefinition::getPrivateProperties)
                    .asInstanceOf(MAP)
                    .isNotEmpty();
        }

        @Test
        void shouldDelete() {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder()
                            .add(VOCAB, EDC_NAMESPACE)
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
                            .add(VOCAB, EDC_NAMESPACE)
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
                    .add("prohibition", createArrayBuilder()
                            .add(createObjectBuilder()
                                    .add("target", "http://example.com/data:77")
                                    .add("action", "index")
                                    .add("remedy", createObjectBuilder()
                                            .add("action", "anonymize"))
                            ))
                    .build();
        }

        private PolicyDefinitionStore store() {
            return runtime.getContext().getService(PolicyDefinitionStore.class);
        }

    }

}
