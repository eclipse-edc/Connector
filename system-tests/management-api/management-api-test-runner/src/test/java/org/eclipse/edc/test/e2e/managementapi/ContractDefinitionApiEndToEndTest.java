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

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.JsonArray;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class ContractDefinitionApiEndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        @Test
        void queryContractDefinitions_noQuerySpec(ManagementEndToEndTestContext context, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createContractDefinition(id).build());

            var body = context.baseRequest()
                    .contentType(JSON)
                    .post("/v3/contractdefinitions/request")
                    .then()
                    .statusCode(200)
                    .body("size()", greaterThan(0))
                    .extract().body().as(JsonArray.class);

            var assetsSelector = body.stream().map(JsonValue::asJsonObject)
                    .filter(it -> it.getString(ID).equals(id))
                    .map(it -> it.getJsonArray("assetsSelector"))
                    .findAny();

            assertThat(assetsSelector).isPresent().get().asInstanceOf(LIST).hasSize(2);
        }

        @Test
        void queryPolicyDefinitionWithSimplePrivateProperties(ManagementEndToEndTestContext context) {
            var id = UUID.randomUUID().toString();
            var requestJson = createDefinitionBuilder(id)
                    .add("privateProperties", createObjectBuilder()
                            .add("newKey", createObjectBuilder().add(ID, "newValue"))
                            .build())
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(requestJson)
                    .post("/v3/contractdefinitions")
                    .then()
                    .statusCode(200)
                    .body("@id", equalTo(id));

            var matchingQuery = context.query(
                    criterion("id", "=", id),
                    criterion("privateProperties.'%snewKey'.@id".formatted(EDC_NAMESPACE), "=", "newValue")
            );

            context.baseRequest()
                    .body(matchingQuery)
                    .contentType(JSON)
                    .post("/v3/contractdefinitions/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1));


            var nonMatchingQuery = context.query(
                    criterion("id", "=", id),
                    criterion("privateProperties.'%snewKey'.@id".formatted(EDC_NAMESPACE), "=", "anything-else")
            );

            context.baseRequest()
                    .body(nonMatchingQuery)
                    .contentType(JSON)
                    .post("/v3/contractdefinitions/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(0));
        }

        @Test
        void queryContractDefinitions_sortByCreatedDate(ManagementEndToEndTestContext context, ContractDefinitionStore store) throws JsonProcessingException {
            var id1 = UUID.randomUUID().toString();
            var id2 = UUID.randomUUID().toString();
            var id3 = UUID.randomUUID().toString();
            var createdAtTime = new AtomicLong(1000L);
            Stream.of(id1, id2, id3).forEach(id -> store.save(createContractDefinition(id).createdAt(createdAtTime.getAndIncrement()).build()));

            var query = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                    .add(TYPE, EDC_NAMESPACE + "QuerySpec")
                    .add("sortField", "createdAt")
                    .add("sortOrder", "DESC")
                    .add("limit", 100)
                    .add("offset", 0)
                    .build();

            var result = context.baseRequest()
                    .contentType(JSON)
                    .body(query)
                    .post("/v3/contractdefinitions/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(3))
                    .extract()
                    .as(List.class);

            assertThat(result)
                    .extracting(cd -> ((LinkedHashMap<?, ?>) cd).get(ID))
                    .containsExactlyElementsOf(List.of(id3, id2, id1));
        }

        @Test
        void shouldCreateAndRetrieve(ManagementEndToEndTestContext context, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var requestJson = createDefinitionBuilder(id)
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(requestJson)
                    .post("/v3/contractdefinitions")
                    .then()
                    .statusCode(200)
                    .body("@id", equalTo(id));

            var actual = store.findById(id);

            assertThat(actual.getId()).matches(id);
        }

        @Test
        void delete(ManagementEndToEndTestContext context, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var entity = createContractDefinition(id).build();
            store.save(entity);

            context.baseRequest()
                    .delete("/v3/contractdefinitions/" + id)
                    .then()
                    .statusCode(204);

            var actual = store.findById(id);

            assertThat(actual).isNull();
        }

        @Test
        void update_whenExists(ManagementEndToEndTestContext context, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var entity = createContractDefinition(id).build();
            store.save(entity);

            var updated = createDefinitionBuilder(id)
                    .add("accessPolicyId", "new-policy")
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(updated)
                    .put("/v3/contractdefinitions")
                    .then()
                    .statusCode(204);

            var actual = store.findById(id);

            assertThat(actual.getAccessPolicyId()).isEqualTo("new-policy");
        }

        @Test
        void update_whenNotExists(ManagementEndToEndTestContext context) {
            var updated = createDefinitionBuilder(UUID.randomUUID().toString())
                    .add("accessPolicyId", "new-policy")
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(updated)
                    .put("/v3/contractdefinitions")
                    .then()
                    .statusCode(404);
        }

        private JsonObjectBuilder createDefinitionBuilder(String id) {
            return createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                    .add(TYPE, EDC_NAMESPACE + "ContractDefinition")
                    .add(ID, id)
                    .add("accessPolicyId", UUID.randomUUID().toString())
                    .add("contractPolicyId", UUID.randomUUID().toString())
                    .add("assetsSelector", createArrayBuilder()
                            .add(createCriterionBuilder("foo", "=", "bar"))
                            .add(createCriterionBuilder("bar", "=", "baz")).build());
        }

        private JsonObjectBuilder createCriterionBuilder(String left, String operator, String right) {
            return createObjectBuilder()
                    .add(TYPE, "Criterion")
                    .add("operandLeft", left)
                    .add("operator", operator)
                    .add("operandRight", right);
        }

        private ContractDefinition.Builder createContractDefinition(String id) {
            return ContractDefinition.Builder.newInstance()
                    .id(id)
                    .accessPolicyId(UUID.randomUUID().toString())
                    .contractPolicyId(UUID.randomUUID().toString())
                    .participantContextId("participantContextId")
                    .assetsSelectorCriterion(criterion("foo", "=", "bar"))
                    .assetsSelectorCriterion(criterion("bar", "=", "baz"));
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

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        @Order(0)
        static PostgresqlEndToEndExtension postgres = new PostgresqlEndToEndExtension();

        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(postgres::config)
                .paramProvider(ManagementEndToEndTestContext.class, ManagementEndToEndTestContext::forContext)
                .build();

    }

}
