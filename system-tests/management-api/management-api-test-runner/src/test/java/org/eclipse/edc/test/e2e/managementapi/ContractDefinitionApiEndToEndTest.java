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
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.query.QuerySpec;
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
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@EndToEndTest
public class ContractDefinitionApiEndToEndTest extends BaseManagementApiEndToEndTest {
    public static final String TEST_ID = "test-id";
    public static final String TEST_AP_ID = "ap1";
    public static final String TEST_CP_ID = "cp1";

    @Test
    void queryContractDefinitions_noQuerySpec() {

        var contractDefStore = controlPlane.getContext().getService(ContractDefinitionStore.class);
        contractDefStore.save(createContractDefinition().build());

        var body = baseRequest()
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .extract().body().as(JsonArray.class);

        var criteria = body.getJsonObject(0).getJsonArray("edc:assetsSelector");
        assertThat(criteria).hasSize(2);
    }

    @Test
    void createContractDef() {
        var defStore = controlPlane.getContext().getService(ContractDefinitionStore.class);
        var requestJson = createDefinitionBuilder()
                .build();

        baseRequest()
                .contentType(JSON)
                .body(requestJson)
                .post()
                .then()
                .statusCode(200)
                .body("@id", equalTo(TEST_ID));

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

        var updated = createDefinitionBuilder()
                .add("accessPolicyId", "new-policy")
                .build();

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

        var updated = createDefinitionBuilder()
                .add("accessPolicyId", "new-policy")
                .build();

        baseRequest()
                .contentType(JSON)
                .body(updated)
                .put()
                .then()
                .statusCode(404);

        var all = store.findAll(QuerySpec.none());
        assertThat(all).isEmpty();
    }

    private RequestSpecification baseRequest() {
        return given()
                .port(PORT)
                .basePath("/management/v2/contractdefinitions")
                .when();
    }

    private JsonObjectBuilder createDefinitionBuilder() {
        return createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, EDC_NAMESPACE + "ContractDefinition")
                .add(ID, TEST_ID)
                .add("accessPolicyId", TEST_AP_ID)
                .add("contractPolicyId", TEST_CP_ID)
                .add("assetsSelector", createArrayBuilder()
                        .add(createCriterionBuilder("foo", "=", "bar"))
                        .add(createCriterionBuilder("bar", "=", "baz")).build());
    }

    private static JsonObjectBuilder createCriterionBuilder(String left, String operator, String right) {
        return createObjectBuilder()
                .add(TYPE, "CriterionDto")
                .add("operandLeft", left)
                .add("operator", operator)
                .add("operandRight", right);
    }

    private ContractDefinition.Builder createContractDefinition() {
        return ContractDefinition.Builder.newInstance()
                .id(TEST_ID)
                .accessPolicyId(TEST_AP_ID)
                .contractPolicyId(TEST_CP_ID)
                .assetsSelectorCriterion(criterion("foo", "=", "bar"))
                .assetsSelectorCriterion(criterion("bar", "=", "baz"));
    }
}
