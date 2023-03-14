/*
 *  Copyright (c) 2022 - 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.connector.api.management.asset;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetCreationRequestDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryDto;
import org.eclipse.edc.connector.api.management.asset.model.DataAddressDto;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@ApiTest
@ExtendWith(EdcExtension.class)
public class AssetApiControllerIntegrationTest {

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
    void getAllAssets(AssetIndex assetIndex) {
        var asset = Asset.Builder.newInstance().id("id").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetIndex.create(asset, dataAddress);

        baseRequest()
                .get("/assets")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void getAllAssetsQuery(AssetIndex assetIndex) {
        var asset = Asset.Builder.newInstance().id("id").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetIndex.create(asset, dataAddress);

        baseRequest()
                .get("/assets?limit=1&offset=0&filter=asset:prop:id=id&sort=DESC&sortField=properties.asset:prop:id")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void getAll_invalidQuery() {
        baseRequest()
                .get("/assets?limit=0&offset=-1&filter=&sortField=")
                .then()
                .statusCode(400);
    }

    @Test
    void queryAllAssets(AssetIndex assetIndex) {
        var asset = Asset.Builder.newInstance().id("id").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetIndex.create(asset, dataAddress);

        baseRequest()
                .contentType(JSON)
                .post("/assets/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void queryAllAssetsQuery(AssetIndex assetIndex) {
        var asset = Asset.Builder.newInstance().id("id").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetIndex.create(asset, dataAddress);

        baseRequest()
                .contentType(JSON)
                .body(QuerySpecDto.Builder.newInstance().limit(1).offset(0).filterExpression(List.of(CriterionDto.from("asset:prop:id", "=", "id"))).sortOrder(SortOrder.DESC).sortField("properties.asset:prop:id"))
                .post("/assets/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void queryAll_noResults(AssetIndex assetIndex) {
        var asset = Asset.Builder.newInstance().id("id").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetIndex.create(asset, dataAddress);

        baseRequest()
                .contentType(JSON)
                .body(QuerySpecDto.Builder.newInstance().limit(1).offset(0).filterExpression(List.of(CriterionDto.from("asset:prop:id", "=", "notexist"))).sortOrder(SortOrder.DESC).sortField("properties.asset:prop:id"))
                .post("/assets/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void getSingleAsset(AssetIndex assetIndex) {
        var asset = Asset.Builder.newInstance().id("id").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetIndex.create(asset, dataAddress);

        baseRequest()
                .get("/assets/id")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("properties.size()", greaterThan(0));
    }

    @Test
    void getSingleAsset_notFound() {
        baseRequest()
                .get("/assets/not-existent-id")
                .then()
                .statusCode(404);
    }

    @Test
    void postAsset(AssetIndex assetIndex) {
        var assetEntryDto = createAssetEntryDto("assetId");

        baseRequest()
                .body(assetEntryDto)
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", is("assetId"))
                .body("createdAt", not("0"));
        assertThat(assetIndex.findById("assetId")).isNotNull();
    }

    @Test
    void postAsset_supportOldIdAsPropertyApi(AssetIndex assetIndex) {
        var assetDto = AssetCreationRequestDto.Builder.newInstance().properties(Map.of(Asset.PROPERTY_ID, "assetId", "Asset-1", "An Asset")).build();
        var dataAddress = DataAddressDto.Builder.newInstance().properties(Map.of("type", "type", "asset-1", "/localhost")).build();
        var assetEntryDto = AssetEntryDto.Builder.newInstance().asset(assetDto).dataAddress(dataAddress).build();

        baseRequest()
                .body(assetEntryDto)
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", is("assetId"));
        assertThat(assetIndex.findById("assetId")).isNotNull();
    }

    @Test
    void postAsset_invalidBody_nullDataAddress(AssetIndex assetIndex) {
        var assetDto = AssetCreationRequestDto.Builder.newInstance().id("assetId").properties(Map.of("Asset-1", "An Asset")).build();
        var assetEntryDto = AssetEntryDto.Builder.newInstance().asset(assetDto).dataAddress(null).build();

        baseRequest()
                .body(assetEntryDto)
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(400);
        assertThat(assetIndex.findById("assetId")).isNull();
    }

    @Test
    void postAssetId_alreadyExists(AssetIndex assetIndex) {
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetIndex.create(asset, dataAddress);
        var assetEntryDto = createAssetEntryDto("assetId");

        baseRequest()
                .body(assetEntryDto)
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(409);
    }

    @Test
    void postAsset_emptyAttributes() {
        var assetEntryDto = createAssetEntryDto_emptyAttributes();

        baseRequest()
                .body(assetEntryDto)
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(400);
    }

    @Test
    void deleteAsset(AssetIndex assetIndex) {
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetIndex.create(asset, dataAddress);

        baseRequest()
                .contentType(JSON)
                .delete("/assets/assetId")
                .then()
                .statusCode(204);
        assertThat(assetIndex.findById("assetId")).isNull();
    }

    @Test
    void deleteAsset_notExists() {
        baseRequest()
                .contentType(JSON)
                .delete("/assets/not-existent-id")
                .then()
                .statusCode(404);
    }

    @Test
    void deleteAsset_alreadyReferencedInAgreement(ContractNegotiationStore negotiationStore, AssetIndex assetIndex) {
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetIndex.create(asset, dataAddress);
        negotiationStore.save(createContractNegotiation(asset));

        baseRequest()
                .contentType(JSON)
                .delete("/assets/assetId")
                .then()
                .statusCode(409);
    }

    @Test
    void getAssetAddress(AssetIndex assetIndex) {
        var asset = Asset.Builder.newInstance().id("id").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetIndex.create(asset, dataAddress);

        baseRequest()
                .get("/assets/id/address")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("properties.size()", greaterThan(0));
    }

    @Test
    void getAssetAddress_notFound() {
        baseRequest()
                .get("/assets/not-existent-id/address")
                .then()
                .statusCode(404);
    }

    @Test
    void updateAsset_whenExists(AssetIndex assetIndex) {
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetIndex.create(asset, dataAddress);

        asset.getProperties().put("anotherKey", "anotherVal");

        baseRequest()
                .body(asset)
                .contentType(JSON)
                .put("/assets/assetId")
                .then()
                .statusCode(204);

        // verify by looking at the asset index
        var found = assetIndex.findById("assetId");
        assertThat(found).isNotNull();
        assertThat(found.getProperties()).containsEntry("anotherKey", "anotherVal");
    }

    @Test
    void updateAsset_whenNotExists(AssetIndex assetIndex) {
        var asset = Asset.Builder.newInstance().id("assetId").build();

        baseRequest()
                .body(asset)
                .contentType(JSON)
                .put("/assets/assetId")
                .then()
                .statusCode(404);

        assertThat(assetIndex.findById("assetId")).isNull();
    }

    @Test
    void updateDataAddress_whenAssetExists(AssetIndex assetIndex, DataAddressResolver resolver) {
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetIndex.create(asset, dataAddress);

        dataAddress.getProperties().put("anotherKey", "anotherVal");

        baseRequest()
                .body(dataAddress)
                .contentType(JSON)
                .put("/assets/assetId/dataaddress")
                .then()
                .statusCode(204);

        // verify by looking at the asset index
        var found = resolver.resolveForAsset("assetId");
        assertThat(found).isNotNull();
        assertThat(found.getProperties()).containsEntry("anotherKey", "anotherVal");
    }

    @Test
    void updateDataAddress_whenNotExists(DataAddressResolver resolver) {
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();

        baseRequest()
                .body(dataAddress)
                .contentType(JSON)
                .put("/assets/assetId/dataaddress")
                .then()
                .statusCode(404);

        assertThat(resolver.resolveForAsset("assetId")).isNull();
    }

    private ContractNegotiation createContractNegotiation(Asset asset) {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .counterPartyId(UUID.randomUUID().toString())
                .counterPartyAddress("address")
                .protocol("protocol")
                .contractAgreement(createContractAgreement(asset))
                .build();
    }

    private ContractAgreement createContractAgreement(Asset asset) {
        return ContractAgreement.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .providerAgentId(UUID.randomUUID().toString())
                .consumerAgentId(UUID.randomUUID().toString())
                .assetId(asset.getId())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/api/v1/management")
                .header("x-api-key", authKey)
                .when();
    }

    private AssetEntryDto createAssetEntryDto(String id) {
        var assetDto = AssetCreationRequestDto.Builder.newInstance().id(id).properties(Map.of("Asset-1", "An Asset")).build();
        var dataAddress = DataAddressDto.Builder.newInstance().properties(Map.of("type", "type", "asset-1", "/localhost")).build();
        return AssetEntryDto.Builder.newInstance().asset(assetDto).dataAddress(dataAddress).build();
    }

    private AssetEntryDto createAssetEntryDto_emptyAttributes() {
        var assetDto = AssetCreationRequestDto.Builder.newInstance().properties(Collections.singletonMap("", "")).build();
        var dataAddress = DataAddressDto.Builder.newInstance().properties(Collections.singletonMap("", "")).build();
        return AssetEntryDto.Builder.newInstance().asset(assetDto).dataAddress(dataAddress).build();
    }
}
