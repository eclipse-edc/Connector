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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionDto;
import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.Matchers.is;

@ExtendWith(EdcExtension.class)
public class ContractDefinitionApiControllerIntegrationTest {

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
    void getAllContractDefs(ContractDefinitionLoader loader) {
        loader.accept(createContractDefinition("definitionId"));

        baseRequest()
                .get("/contractdefinitions")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void getAllContractDefs_withPaging(ContractDefinitionLoader loader) {
        loader.accept(createContractDefinition("definitionId"));

        baseRequest()
                .get("/contractdefinitions?offset=0&limit=15&sort=ASC")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void getAll_invalidQuery() {
        baseRequest()
                .get("/contractdefinitions?limit=1&offset=-1&filter=&sortField=")
                .then()
                .statusCode(400);
    }

    @Test
    void getSingleContractDef(ContractDefinitionLoader loader) {
        loader.accept(createContractDefinition("definitionId"));

        baseRequest()
                .get("/contractdefinitions/definitionId")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", is("definitionId"));
    }

    @Test
    void getSingleContractDef_notFound() {
        baseRequest()
                .get("/contractdefinitions/nonExistingId")
                .then()
                .statusCode(404);
    }

    @Test
    void postContractDefinition(ContractDefinitionStore store) {
        var dto = createDto("definitionId");

        baseRequest()
                .body(dto)
                .contentType(JSON)
                .post("/contractdefinitions")
                .then()
                .statusCode(204);
        assertThat(store.findAll()).isNotEmpty();
    }

    @Test
    void postContractDefinition_invalidBody(ContractDefinitionStore store) {
        var dto = ContractDefinitionDto.Builder.newInstance()
                .id("test-id")
                .contractPolicyId(null)
                .accessPolicyId(UUID.randomUUID().toString())
                .build();

        baseRequest()
                .body(dto)
                .contentType(JSON)
                .post("/contractdefinitions")
                .then()
                .statusCode(400);
        assertThat(store.findAll()).isEmpty();
    }

    @Test
    void postContractDefinition_alreadyExists(ContractDefinitionLoader loader, ContractDefinitionStore store) {
        loader.accept(createContractDefinition("definitionId"));
        var dto = createDto("definitionId");

        baseRequest()
                .body(dto)
                .contentType(JSON)
                .post("/contractdefinitions")
                .then()
                .statusCode(409);
        assertThat(store.findAll()).hasSize(1);
    }

    @Test
    void deleteContractDefinition(ContractDefinitionLoader loader, ContractDefinitionStore store) {
        loader.accept(createContractDefinition("definitionId"));

        baseRequest()
                .contentType(JSON)
                .delete("/contractdefinitions/definitionId")
                .then()
                .statusCode(204);
        assertThat(store.findAll()).isEmpty();
    }

    @Test
    void deleteContractDefinition_notExists() {
        baseRequest()
                .contentType(JSON)
                .delete("/contractdefinitions/nonExistingId")
                .then()
                .statusCode(404);
    }

    private ContractDefinitionDto createDto(String definitionId) {
        return ContractDefinitionDto.Builder.newInstance()
                .id(definitionId)
                .contractPolicyId(UUID.randomUUID().toString())
                .accessPolicyId(UUID.randomUUID().toString())
                .build();
    }

    private ContractDefinition createContractDefinition(String id) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicyId(UUID.randomUUID().toString())
                .contractPolicyId(UUID.randomUUID().toString())
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
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
