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

package org.eclipse.edc.test.e2e.managementapi.v5;

import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.jsonLdContext;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;

@ApiTest
public class ContractDefinitionApiV5EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {
        private static final String PARTICIPANT_CONTEXT_ID = "test-participant";

        private String participantTokenJwt;

        @BeforeEach
        void setup(OauthServer authServer, ParticipantContextService participantContextService) {
            createParticipant(participantContextService, PARTICIPANT_CONTEXT_ID);

            participantTokenJwt = authServer.createToken(PARTICIPANT_CONTEXT_ID);

        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService) {
            participantContextService.deleteParticipantContext(PARTICIPANT_CONTEXT_ID)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        }


        @Test
        void create(ManagementEndToEndV5TestContext context, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var requestJson = createDefinitionBuilder(id)
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestJson)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions")
                    .then()
                    .statusCode(200)
                    .body("@id", equalTo(id));

            var actual = store.findById(id);

            assertThat(actual.getId()).matches(id);
        }

        @Test
        void create_tokenBearerNotOwner(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService srv) {
            var id = UUID.randomUUID().toString();
            var requestJson = createDefinitionBuilder(id)
                    .build()
                    .toString();

            // create a second participant who will make the request
            var otherParticipantId = UUID.randomUUID().toString();
            createParticipant(srv, otherParticipantId);
            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(requestJson)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions")
                    .then()
                    .statusCode(403)
                    .body(containsString("User '%s' is not authorized to access this resource".formatted(otherParticipantId)));
        }

        @Test
        void create_resourceNotOwnedByTokenBearer(ManagementEndToEndV5TestContext context, ParticipantContextService srv) {
            var id = UUID.randomUUID().toString();
            var requestJson = createDefinitionBuilder(id)
                    .build()
                    .toString();

            // create a second participant who will make the request
            var otherParticipantId = UUID.randomUUID().toString();
            createParticipant(srv, otherParticipantId);

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestJson)
                    .post("/v5beta/participants/" + otherParticipantId + "/contractdefinitions")
                    .then()
                    .statusCode(403);
        }

        @Test
        void create_tokenBearerIsAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var requestJson = createDefinitionBuilder(id)
                    .build()
                    .toString();

            context.baseRequest(authServer.createAdminToken())
                    .contentType(JSON)
                    .body(requestJson)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions")
                    .then()
                    .statusCode(200)
                    .body("@id", equalTo(id));

            var actual = store.findById(id);

            assertThat(actual.getId()).matches(id);
        }

        @Test
        void create_tokenLacksRequiredScope(ManagementEndToEndV5TestContext context, OauthServer authServer) {

            var id = UUID.randomUUID().toString();
            var requestJson = createDefinitionBuilder(id)
                    .build()
                    .toString();

            var offendingToken = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:read"));
            context.baseRequest(offendingToken)
                    .contentType(JSON)
                    .body(requestJson)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions")
                    .then()
                    .statusCode(403)
                    .body(matchesRegex("(?s).*Required scope.*management-api:write.*missing.*"));
        }

        @Test
        void create_tokenHasWrongRole(ManagementEndToEndV5TestContext context, OauthServer authServer) {

            var id = UUID.randomUUID().toString();
            var requestJson = createDefinitionBuilder(id)
                    .build()
                    .toString();

            var offendingToken = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("role", "barbaz"));
            context.baseRequest(offendingToken)
                    .contentType(JSON)
                    .body(requestJson)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions")
                    .then()
                    .statusCode(403)
                    .body(matchesRegex("Required user role not satisfied."));
        }

        @Test
        void queryContractDefinitions_noQuerySpec(ManagementEndToEndV5TestContext context, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createContractDefinition(id).build());

            var body = context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/request")
                    .then()
                    .statusCode(200)
                    .body("size()", greaterThan(0))
                    .extract().body().as(Map[].class);

            var assetsSelector = Arrays.stream(body)
                    .filter(it -> it.get(ID).equals(id))
                    .map(it -> it.get("assetsSelector"))
                    .findAny();

            assertThat(assetsSelector).isPresent().get().asInstanceOf(LIST).hasSize(2);
        }

        @Test
        void queryContractDefinitionWithSimplePrivateProperties(ManagementEndToEndV5TestContext context) {
            var id = UUID.randomUUID().toString();
            var requestJson = createDefinitionBuilder(id)
                    .add("privateProperties", createObjectBuilder()
                            .add("newKey", createObjectBuilder().add(ID, "newValue"))
                            .build())
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestJson)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions")
                    .then()
                    .statusCode(200)
                    .body("@id", equalTo(id));

            var matchingQuery = context.query(
                    criterion("id", "=", id),
                    criterion("privateProperties.'%snewKey'.@id".formatted(EDC_NAMESPACE), "=",
                            "newValue")).toString();

            context.baseRequest(participantTokenJwt)
                    .body(matchingQuery)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(1));

            var nonMatchingQuery = context.query(
                    criterion("id", "=", id),
                    criterion("privateProperties.'%snewKey'.@id".formatted(EDC_NAMESPACE), "=",
                            "anything-else")).toString();

            context.baseRequest(participantTokenJwt)
                    .body(nonMatchingQuery)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(0));
        }

        @Test
        void queryContractDefinitions_sortByCreatedDate(ManagementEndToEndV5TestContext context, ContractDefinitionStore store) {
            var id1 = UUID.randomUUID().toString();
            var id2 = UUID.randomUUID().toString();
            var id3 = UUID.randomUUID().toString();
            var createdAtTime = new AtomicLong(1000L);
            Stream.of(id1, id2, id3).forEach(id -> store.save(createContractDefinition(id)
                    .createdAt(createdAtTime.getAndIncrement()).build()));

            var query = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "QuerySpec")
                    .add("sortField", "createdAt")
                    .add("sortOrder", "DESC")
                    .add("limit", 100)
                    .add("offset", 0)
                    .build()
                    .toString();

            var result = context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(query)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/request")
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
        void query_tokenBearerIsAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createContractDefinition(id).build());

            var body = context.baseRequest(authServer.createAdminToken())
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/request")
                    .then()
                    .statusCode(200)
                    .body("size()", greaterThan(0))
                    .extract().body().as(Map[].class);

            var assetsSelector = Arrays.stream(body)
                    .filter(it -> it.get(ID).equals(id))
                    .map(it -> it.get("assetsSelector"))
                    .findAny();

            assertThat(assetsSelector).isPresent().get().asInstanceOf(LIST).hasSize(2);
        }

        @Test
        void query_shouldLimitToResourceOwner(ManagementEndToEndV5TestContext context, ContractDefinitionStore store) {
            store.save(createContractDefinition("cd-1").build());
            store.save(createContractDefinition("cd-2").build());
            store.save(createContractDefinition("other-cd-1").participantContextId("another-participant").build());
            store.save(createContractDefinition("other-cd-2").participantContextId("another-participant").build());

            var body = context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/request")
                    .then()
                    .statusCode(200)
                    .body("size()", greaterThanOrEqualTo(2))
                    .extract().body().as(Map[].class);


            assertThat(Arrays.stream(body)).anyMatch(e -> e.get(ID).equals("cd-1"));
            assertThat(Arrays.stream(body)).anyMatch(e -> e.get(ID).equals("cd-2"));
            assertThat(Arrays.stream(body)).noneMatch(e -> e.get(ID).equals("another-cd-1"));
            assertThat(Arrays.stream(body)).noneMatch(e -> e.get(ID).equals("another-cd-2"));
        }

        @Test
        void query_tokenBearerNotResourceOwner(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                               ContractDefinitionStore store, ParticipantContextService srv) {
            var id = UUID.randomUUID().toString();
            store.save(createContractDefinition(id).build());

            createParticipant(srv, "another-participant");
            var token = authServer.createToken("another-participant");

            context.baseRequest(token)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/request")
                    .then()
                    .statusCode(403)
                    .body(containsString("User '%s' is not authorized to access this resource".formatted("another-participant")));
        }

        @Test
        void query_tokenLacksScope(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                   ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createContractDefinition(id).build());

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:bizzbuzz"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/request")
                    .then()
                    .statusCode(403)
                    .body(matchesRegex("(?s).*Required scope.*management-api:read.*missing.*"));
        }

        @Test
        void query_tokenHasWrongRole(ManagementEndToEndV5TestContext context, OauthServer authServer, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createContractDefinition(id).build());

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("role", "some-role"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/request")
                    .then()
                    .statusCode(403)
                    .body(containsString("Required user role not satisfied."));
        }

        @Test
        void delete(ManagementEndToEndV5TestContext context, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var entity = createContractDefinition(id).build();
            store.save(entity);

            context.baseRequest(participantTokenJwt)
                    .delete("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/" + id)
                    .then()
                    .statusCode(204);

            var actual = store.findById(id);

            assertThat(actual).isNull();
        }

        @Test
        void delete_tokenBearerNotOwner(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                        ContractDefinitionStore store, ParticipantContextService srv) {
            var id = UUID.randomUUID().toString();
            var entity = createContractDefinition(id)
                    .build();
            store.save(entity).orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            // create a second participant who will make the request
            var otherParticipantId = UUID.randomUUID().toString();
            createParticipant(srv, otherParticipantId);
            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .delete("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/" + id)
                    .then()
                    .statusCode(403)
                    .body(containsString("User '%s' is not authorized to access this resource".formatted(otherParticipantId)));

        }

        @Test
        void delete_resourceNotOwnedByTokenBearer(ManagementEndToEndV5TestContext context, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var otherParticipantId = "other-participant";
            var entity = createContractDefinition(id)
                    .participantContextId(otherParticipantId)
                    .build();
            store.save(entity).orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            //url path != actual owner
            context.baseRequest(participantTokenJwt)
                    .delete("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/" + id)
                    .then()
                    .statusCode(404);

            // url path == actual owner, but token is not authorized to access it
            context.baseRequest(participantTokenJwt)
                    .delete("/v5beta/participants/" + otherParticipantId + "/contractdefinitions/" + id)
                    .then()
                    .statusCode(403);
        }

        @Test
        void delete_tokenBearerIsAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var entity = createContractDefinition(id).build();
            store.save(entity);

            var adminToken = authServer.createAdminToken();
            context.baseRequest(adminToken)
                    .delete("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/" + id)
                    .then()
                    .statusCode(204);

            var actual = store.findById(id);

            assertThat(actual).isNull();
        }

        @Test
        void delete_tokenLacksRequiredScopes(ManagementEndToEndV5TestContext context, OauthServer authServer, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var entity = createContractDefinition(id).build();
            store.save(entity);

            var offendingToken = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:foobar"));

            context.baseRequest(offendingToken)
                    .delete("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/" + id)
                    .then()
                    .statusCode(403);
        }

        @Test
        void delete_tokenHasWrongRole(ManagementEndToEndV5TestContext context, OauthServer authServer, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var entity = createContractDefinition(id).build();
            store.save(entity);

            var offendingToken = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("role", "barbaz"));

            context.baseRequest(offendingToken)
                    .delete("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions/" + id)
                    .then()
                    .statusCode(403);
        }

        @Test
        void update_whenExists(ManagementEndToEndV5TestContext context, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var entity = createContractDefinition(id).build();
            store.save(entity);

            var updated = createDefinitionBuilder(id)
                    .add("accessPolicyId", "new-policy")
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(updated)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions")
                    .then()
                    .statusCode(204);

            var actual = store.findById(id);

            assertThat(actual.getAccessPolicyId()).isEqualTo("new-policy");
        }

        @Test
        void update_whenNotExists(ManagementEndToEndV5TestContext context) {
            var updated = createDefinitionBuilder(UUID.randomUUID().toString())
                    .add("accessPolicyId", "new-policy")
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(updated)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions")
                    .then()
                    .statusCode(404);
        }

        @Test
        void update_tokenBearerNotOwner(ManagementEndToEndV5TestContext context, OauthServer authServer, ContractDefinitionStore store, ParticipantContextService srv) {

            var id = UUID.randomUUID().toString();
            var entity = createContractDefinition(id).build();
            store.save(entity);

            // create a second participant who will make the request
            var otherParticipantId = UUID.randomUUID().toString();
            createParticipant(srv, otherParticipantId);
            var offendingToken = authServer.createToken(otherParticipantId);


            var updated = createDefinitionBuilder(id)
                    .add("accessPolicyId", "new-policy")
                    .build()
                    .toString();

            context.baseRequest(offendingToken)
                    .contentType(JSON)
                    .body(updated)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions")
                    .then()
                    .statusCode(403)
                    .body(containsString("User '%s' is not authorized to access this resource".formatted(otherParticipantId)));
        }

        @Test
        void update_resourceNotOwnedByTokenBearer(ManagementEndToEndV5TestContext context, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var otherParticipantId = "other-participant";
            var entity = createContractDefinition(id)
                    .participantContextId(otherParticipantId)
                    .build();
            store.save(entity);

            var updated = createDefinitionBuilder(id)
                    .add("accessPolicyId", "new-policy")
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(updated)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions")
                    .then()
                    .statusCode(404);

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(updated)
                    .put("/v5beta/participants/" + otherParticipantId + "/contractdefinitions")
                    .then()
                    .statusCode(403);
        }

        @Test
        void update_tokenBearerIsAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var entity = createContractDefinition(id).build();
            store.save(entity);

            var updated = createDefinitionBuilder(id)
                    .add("accessPolicyId", "new-policy")
                    .build()
                    .toString();

            context.baseRequest(authServer.createAdminToken())
                    .contentType(JSON)
                    .body(updated)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions")
                    .then()
                    .statusCode(204);
        }

        @Test
        void update_tokenHasWrongRole(ManagementEndToEndV5TestContext context, OauthServer authServer, ContractDefinitionStore store) {

            var id = UUID.randomUUID().toString();
            var entity = createContractDefinition(id).build();
            store.save(entity);

            var updated = createDefinitionBuilder(id)
                    .add("accessPolicyId", "new-policy")
                    .build()
                    .toString();

            var offendingToken = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("role", "barbaz"));
            context.baseRequest(offendingToken)
                    .contentType(JSON)
                    .body(updated)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions")
                    .then()
                    .statusCode(403);
        }

        @Test
        void update_tokenLacksRequiredScopes(ManagementEndToEndV5TestContext context, OauthServer authServer, ContractDefinitionStore store) {
            var id = UUID.randomUUID().toString();
            var entity = createContractDefinition(id).build();
            store.save(entity);

            var updated = createDefinitionBuilder(id)
                    .add("accessPolicyId", "new-policy")
                    .build()
                    .toString();

            var offendingToken = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:read"));
            context.baseRequest(offendingToken)
                    .contentType(JSON)
                    .body(updated)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractdefinitions")
                    .then()
                    .statusCode(403);
        }

        private JsonObjectBuilder createDefinitionBuilder(String id) {
            return createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "ContractDefinition")
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
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .assetsSelectorCriterion(criterion("foo", "=", "bar"))
                    .assetsSelectorCriterion(criterion("bar", "=", "baz"));
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @Order(0)
        @RegisterExtension
        static final OauthServerEndToEndExtension AUTH_SERVER_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance().build();

        @Order(1)
        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.VIRTUAL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final OauthServerEndToEndExtension AUTH_SERVER_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance().build();

        @RegisterExtension
        @Order(0)
        static final PostgresqlEndToEndExtension POSTGRES_EXTENSION = new PostgresqlEndToEndExtension();

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback SETUP = context -> {
            POSTGRES_EXTENSION.createDatabase(Runtimes.ControlPlane.NAME.toLowerCase());
        };

        @Order(2)
        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.VIRTUAL_MODULES)
                .modules(Runtimes.ControlPlane.VIRTUAL_SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(() -> POSTGRES_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();

    }

}