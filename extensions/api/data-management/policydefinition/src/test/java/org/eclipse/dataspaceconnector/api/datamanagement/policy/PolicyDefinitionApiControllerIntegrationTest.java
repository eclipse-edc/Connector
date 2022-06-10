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

package org.eclipse.dataspaceconnector.api.datamanagement.policy;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.api.datamanagement.policy.TestFunctions.createPolicy;
import static org.eclipse.dataspaceconnector.api.datamanagement.policy.TestFunctions.createSelectorExpression;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.Matchers.is;

@ExtendWith(EdcExtension.class)
public class PolicyDefinitionApiControllerIntegrationTest {

    private final int port = getFreePort();
    private final String authKey = "123456";

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.data.port", String.valueOf(port),
                "web.http.data.path", "/api/v1/data",
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
    void getSinglePolicy(PolicyDefinitionStore policyStore) {
        var policy = createPolicy("id");
        policyStore.save(policy);

        baseRequest()
                .get("/policydefinitions/id")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("uid", is("id"));
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
                .body(createPolicy("id"))
                .contentType(JSON)
                .post("/policydefinitions")
                .then()
                .statusCode(204);
        assertThat(policyStore.findById("id")).isNotNull();
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
                .basePath("/api/v1/data")
                .header("x-api-key", authKey)
                .when();
    }
}
