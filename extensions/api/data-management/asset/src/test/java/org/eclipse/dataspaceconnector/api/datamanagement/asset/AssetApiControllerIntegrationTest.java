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

package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.api.datamanagement.asset.TestFunctions.createAssetEntryDto;
import static org.eclipse.dataspaceconnector.api.datamanagement.asset.TestFunctions.createAssetEntryDto_emptyAttributes;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

@ExtendWith(EdcExtension.class)
public class AssetApiControllerIntegrationTest {

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
    void getAllAssets(AssetLoader assetLoader) {
        var asset = Asset.Builder.newInstance().id("id").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetLoader.accept(asset, dataAddress);

        baseRequest()
                .get("/assets")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void getAllAssetsQuery(AssetLoader assetLoader) {
        var asset = Asset.Builder.newInstance().id("id").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetLoader.accept(asset, dataAddress);

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
    void getSingleAsset(AssetLoader assetLoader) {
        var asset = Asset.Builder.newInstance().id("id").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetLoader.accept(asset, dataAddress);

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
                .statusCode(204);
        assertThat(assetIndex.findById("assetId")).isNotNull();
    }

    @Test
    void postAsset_invalidBody(AssetIndex assetIndex) {
        var assetDto = AssetDto.Builder.newInstance().properties(Map.of(Asset.PROPERTY_ID, "testId", "Asset-1", "An Asset")).build();
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
    void postAssetId_alreadyExists(AssetLoader assetLoader) {
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetLoader.accept(asset, dataAddress);
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
    void deleteAsset(AssetLoader assetLoader, AssetIndex assetIndex) {
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetLoader.accept(asset, dataAddress);

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
    void deleteAsset_alreadyReferencedInAgreement(ContractNegotiationStore negotiationStore, AssetLoader assetLoader) {
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assetLoader.accept(asset, dataAddress);
        negotiationStore.save(createContractNegotiation(asset));

        baseRequest()
                .contentType(JSON)
                .delete("/assets/assetId")
                .then()
                .statusCode(409);
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
                .basePath("/api/v1/data")
                .header("x-api-key", authKey)
                .when();
    }
}
