/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.policy;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.query.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.api.management.policy.TestFunctions.createPolicy;
import static org.eclipse.edc.connector.api.management.policy.TestFunctions.createSelectorExpression;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@ApiTest
@ExtendWith(EdcExtension.class)
public class PolicyDefinitionApiControllerIntegrationTest {

    private final int port = getFreePort();
    private final String authKey = "123456";

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.management.port", String.valueOf(port),
                "web.http.management.path", "/api/v1/management",
                "edc.api.auth.key", authKey
        ));
    }

    @Test
    void getAllpolicydefinitions(PolicyDefinitionStore policyStore) {
        var policy = createPolicy("id");

        policyStore.save(policy);

        baseRequest()
                .get("/policydefinitions")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(1));
    }

    @Test
    void getAll_invalidQuery() {
        baseRequest()
                .get("/policydefinitions?limit=1&offset=-1&filter=&sortField=")
                .then()
                .statusCode(400);
    }

    @Test
    void queryAllPolicyDefinitions(PolicyDefinitionStore policyStore) {
        var policy = createPolicy("id");

        policyStore.save(policy);

        baseRequest()
                .contentType(JSON)
                .post("/policydefinitions/request")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(1));
    }

    @Test
    void queryAllPolicyDefinitions_withQuery(PolicyDefinitionStore policyStore) {
        IntStream.range(0, 10).forEach(i -> policyStore.save(createPolicy("id" + i)));

        baseRequest()
                .contentType(JSON)
                .body(QuerySpecDto.Builder.newInstance()
                        .limit(10)
                        .offset(0)
                        .sortOrder(SortOrder.ASC)
                        .filterExpression(List.of(CriterionDto.from("id", "in", List.of("id3", "id2"))))
                        .build())
                .post("/policydefinitions/request")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(2));
    }

    @Test
    void queryAllPolicyDefinitions_withPaging(PolicyDefinitionStore policyStore) {
        IntStream.range(0, 10).forEach(i -> policyStore.save(createPolicy("id" + i)));

        baseRequest()
                .contentType(JSON)
                .body(QuerySpecDto.Builder.newInstance()
                        .limit(10)
                        .offset(0)
                        .sortOrder(SortOrder.DESC)
                        .sortField("id")
                        .build())
                .post("/policydefinitions/request")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(10));
    }

    @Test
    void getSinglePolicy(PolicyDefinitionStore policyStore) {
        var policy = createPolicy("id");
        policyStore.save(policy);

        baseRequest()
                .get("/policydefinitions/id")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", is("id"));
    }

    @Test
    void getSinglePolicy_notFound() {
        baseRequest()
                .get("/policydefinitions/not-existent-id")
                .then()
                .statusCode(404);
    }

    @Test
    void postPolicy(PolicyDefinitionStore policyStore) {

        baseRequest()
                .body(createPolicy("policydefinitionId"))
                .contentType(JSON)
                .post("/policydefinitions")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", is("policydefinitionId"))
                .body("createdAt", not("0"));

        assertThat(policyStore.findById("policydefinitionId")).isNotNull();
    }

    @Test
    void postPolicyId_alreadyExists(PolicyDefinitionStore policyStore) {
        policyStore.save(createPolicy("id"));

        baseRequest()
                .body(createPolicy("id"))
                .contentType(JSON)
                .post("/policydefinitions")
                .then()
                .statusCode(409);
    }

    @Test
    void deletePolicy(PolicyDefinitionStore policyStore) {
        var policy = createPolicy("id");

        policyStore.save(policy);

        baseRequest()
                .contentType(JSON)
                .delete("/policydefinitions/id")
                .then()
                .statusCode(204);
        assertThat(policyStore.findById("id")).isNull();
    }

    @Test
    void deletePolicy_notExists() {
        baseRequest()
                .contentType(JSON)
                .delete("/policydefinitions/not-existent-id")
                .then()
                .statusCode(404);
    }

    @Test
    void deletePolicy_ExistsInContractDefinitionNotExistsInPolicyStore(ContractDefinitionStore contractDefinitionStore) {
        var policy = createPolicy("access");
        contractDefinitionStore.save(createContractDefinition(policy.getUid()));
        baseRequest()
                .contentType(JSON)
                .delete("/policydefinitions/access")
                .then()
                .statusCode(404);
    }

    @Test
    void deletePolicy_alreadyReferencedInContractDefinition(ContractDefinitionStore contractDefinitionStore, PolicyDefinitionStore policyStore) {
        var policy = createPolicy("access");
        policyStore.save(policy);
        contractDefinitionStore.save(createContractDefinition(policy.getUid()));

        baseRequest()
                .contentType(JSON)
                .delete("/policydefinitions/access")
                .then()
                .statusCode(409);
    }


    private ContractDefinition createContractDefinition(String accessPolicyId) {
        return ContractDefinition.Builder.newInstance()
                .id("definition")
                .contractPolicyId("contract")
                .accessPolicyId(accessPolicyId)
                .selectorExpression(createSelectorExpression())
                .build();
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/api/v1/management")
                .header("x-api-key", authKey)
                .when();
    }
}
