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

import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@EndToEndTest
public class ContractDefinitionApiEndToEndTest extends BaseManagementApiEndToEndTest {
    public static final String TEST_ID = "test-id";
    public static final String TEST_AP_ID = "ap1";
    public static final String TEST_CP_ID = "cp1";
    // These constants should be equal to the ones found ContractDefinitionRequestDto!
    public static final String CONTRACT_DEFINITION_TYPE = EDC_NAMESPACE + "ContractDefinition";
    public static final String CONTRACT_DEFINITION_ACCESSPOLICY_ID = EDC_NAMESPACE + "accessPolicyId";
    public static final String CONTRACT_DEFINITION_CONTRACTPOLICY_ID = EDC_NAMESPACE + "contractPolicyId";
    public static final String CONTRACT_DEFINITION_VALIDITY = EDC_NAMESPACE + "validity";
    public static final String CONTRACT_DEFINITION_CRITERIA = EDC_NAMESPACE + "criteria";
    private static final TypeRef<List<JsonObject>> LIST_TYPE = new TypeRef<>() {
    };

    @Test
    void queryContractDefinitions_noQuerySpec() {

        var contractDefStore = controlPlane.getContext().getService(ContractDefinitionStore.class);
        contractDefStore.save(createContractDefinition().build());

        var body = baseRequest()
                .contentType(JSON)
                .body("{}")
                .post("/request")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .extract().body().as(LIST_TYPE);

        var criteria = body.get(0).getJsonArray("edc:criteria");
        assertThat(criteria).hasSize(2);
    }

    @Test
    void createContractDef() {
        var defStore = controlPlane.getContext().getService(ContractDefinitionStore.class);
        var requestJson = createJsonBuilder()
                .build();

        baseRequest()
                .contentType(JSON)
                .body(requestJson)
                .post()
                .then()
                .statusCode(200)
                .body("id", equalTo(TEST_ID));

        assertThat(defStore.findAll(QuerySpec.none())).hasSize(1)
                .allMatch(cd -> cd.getId().equals(TEST_ID));
    }

    @Test
    void delete() {
        var store = controlPlane.getContext().getService(ContractDefinitionStore.class);
        var entity = createContractDefinition().build();
        store.save(entity);

        baseRequest()
                .delete(entity.getId())
                .then()
                .statusCode(204);

        assertThat(store.findAll(QuerySpec.none())).isEmpty();
    }

    @Test
    void update_whenExists() {
        var store = controlPlane.getContext().getService(ContractDefinitionStore.class);
        var entity = createContractDefinition().build();
        store.save(entity);

        var updated = createJsonBuilder().add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, "new-policy").build();
        baseRequest()
                .contentType(JSON)
                .body(updated)
                .put()
                .then()
                .statusCode(204);

        var all = store.findAll(QuerySpec.none());
        assertThat(all).hasSize(1)
                .allSatisfy(cd -> assertThat(cd.getAccessPolicyId()).isEqualTo("new-policy"));
    }

    @Test
    void update_whenNotExists() {
        var store = controlPlane.getContext().getService(ContractDefinitionStore.class);
        // nothing is saved in the store, so the update will fail

        var updated = createJsonBuilder().add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, "new-policy").build();
        baseRequest()
                .contentType(JSON)
                .body(updated)
                .put()
                .then()
                .statusCode(404);

        var all = store.findAll(QuerySpec.none());
        assertThat(all).isEmpty();
    }

    private JsonObjectBuilder createJsonBuilder() {
        return Json.createObjectBuilder()
                .add(TYPE, CONTRACT_DEFINITION_TYPE)
                .add(ID, TEST_ID)
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, TEST_AP_ID)
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, TEST_CP_ID)
                .add(CONTRACT_DEFINITION_VALIDITY, 3600)
                .add(CONTRACT_DEFINITION_CRITERIA, createCriterionBuilder().build());
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + PORT + "/management/v2/contractdefinitions")
                .when();
    }

    private JsonArrayBuilder createCriterionBuilder() {
        return Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add(TYPE, EDC_NAMESPACE + "CriterionDto")
                        .add(EDC_NAMESPACE + "operandLeft", "foo")
                        .add(EDC_NAMESPACE + "operator", "=")
                        .add(EDC_NAMESPACE + "operandRight", "bar")
                )
                .add(Json.createObjectBuilder()
                        .add(TYPE, EDC_NAMESPACE + "CriterionDto")
                        .add(EDC_NAMESPACE + "operandLeft", "bar")
                        .add(EDC_NAMESPACE + "operator", "=")
                        .add(EDC_NAMESPACE + "operandRight", "baz")
                );
    }

    private ContractDefinition.Builder createContractDefinition() {
        return ContractDefinition.Builder.newInstance()
                .id(TEST_ID)
                .accessPolicyId(TEST_AP_ID)
                .contractPolicyId(TEST_CP_ID)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance()
                        .criteria(List.of(new Criterion("foo", "=", "bar"),
                                new Criterion("bar", "=", "baz"))).build())
                .validity(TimeUnit.MINUTES.toSeconds(10));
    }
}
