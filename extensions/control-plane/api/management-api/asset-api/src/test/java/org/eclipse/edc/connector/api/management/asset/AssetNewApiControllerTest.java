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

package org.eclipse.edc.connector.api.management.asset;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetResponseDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetResponseNewDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetUpdateRequestDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetUpdateRequestWrapperDto;
import org.eclipse.edc.connector.api.management.asset.model.DataAddressDto;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.EDC_ASSET_TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class AssetNewApiControllerTest extends RestControllerTestBase {

    private static final String TEST_ASSET_ID = "test-asset-id";
    private static final String TEST_ASSET_CONTENTTYPE = "application/json";
    private static final String TEST_ASSET_DESCRIPTION = "test description";
    private static final String TEST_ASSET_VERSION = "0.4.2";
    private static final String TEST_ASSET_NAME = "test-asset";
    private final AssetService service = mock(AssetService.class);
    private final DataAddressResolver dataAddressResolver = mock(DataAddressResolver.class);
    private final TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);

    @Test
    void requestAsset() {
        when(service.query(any()))
                .thenReturn(ServiceResult.success(Stream.of(Asset.Builder.newInstance().build())));
        when(transformerRegistry.transform(isA(Asset.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createAssetJson().build()));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));

        baseRequest()
                .contentType(JSON)
                .post("/assets/request")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
        verify(service).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(isA(Asset.class), eq(JsonObject.class));
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void requestAsset_filtersOutFailedTransforms() {
        when(service.query(any()))
                .thenReturn(ServiceResult.success(Stream.of(Asset.Builder.newInstance().build())));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(Asset.class), eq(JsonObject.class)))
                .thenReturn(Result.failure("failed to transform"));

        baseRequest()
                .contentType(JSON)
                .post("/assets/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(0));
    }

    @Test
    void requestAsset_shouldReturnBadRequest_whenQueryIsInvalid() {
        baseRequest()
                .body(Map.of("offset", -1))
                .contentType(JSON)
                .post("/assets/request")
                .then()
                .statusCode(400);
    }

    @Test
    void requestAsset_shouldReturnBadRequest_whenQueryTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("error"));
        when(service.query(any())).thenReturn(ServiceResult.success());

        baseRequest()
                .contentType(JSON)
                .post("/assets/request")
                .then()
                .statusCode(400);
    }

    @Test
    void requestAsset_shouldReturnBadRequest_whenServiceReturnsBadRequest() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
        when(service.query(any())).thenReturn(ServiceResult.badRequest());

        baseRequest()
                .contentType(JSON)
                .post("/assets/request")
                .then()
                .statusCode(400);
    }

    @Test
    void getSingleAsset() {
        var asset = Asset.Builder.newInstance().property("key", "value").build();
        when(service.findById("id")).thenReturn(asset);
        var jobj = createAssetJson().build();
        when(transformerRegistry.transform(isA(Asset.class), eq(JsonObject.class))).thenReturn(Result.success(jobj));

        var response = baseRequest()
                .get("/assets/id")
                .then()
                .statusCode(200)
                .contentType(JSON);
        var dto = response.extract().body().as(AssetResponseNewDto.class);
        assertThat(dto.getAsset()).isNotNull();
        verify(transformerRegistry).transform(isA(Asset.class), eq(JsonObject.class));
    }

    @Test
    void getSingleAsset_notFound() {
        when(service.findById(any())).thenReturn(null);

        baseRequest()
                .get("/assets/not-existent-id")
                .then()
                .statusCode(404);
    }

    @Test
    void getAssetById_shouldReturnNotFound_whenTransformFails() {
        when(service.findById("id")).thenReturn(Asset.Builder.newInstance().build());
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetResponseDto.class))).thenReturn(Result.failure("failure"));

        baseRequest()
                .get("/assets/id")
                .then()
                .statusCode(404);
    }

    @Test
    void createAsset() {
        var assetEntry = createAssetEntryDto();
        var asset = createAssetBuilder().build();
        when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(DataAddress.Builder.newInstance().type("any").build()));
        when(service.create(any(), any())).thenReturn(ServiceResult.success(asset));

        var resp = baseRequest()
                .contentType(JSON)
                .body(assetEntry)
                .post("/assets")
                .then();
        resp.statusCode(200)
                .contentType(JSON)
                .body("id", is(TEST_ASSET_ID))
                .body("createdAt", greaterThan(0L));

        verify(transformerRegistry).transform(any(), eq(Asset.class));
        verify(transformerRegistry).transform(any(), eq(DataAddress.class));
        verify(service).create(isA(Asset.class), isA(DataAddress.class));
    }

    @Test
    void createAsset_shouldReturnBadRequest_whenDataAddressIsNull() {
        var assetDto = createAssetJson().build();
        var assetEntryDto = AssetEntryNewDto.Builder.newInstance().asset(assetDto).dataAddress(null).build();

        baseRequest()
                .body(assetEntryDto)
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    void createAsset_shouldReturnBadRequest_whenTransformFails() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.failure("failed"));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.failure("failed"));

        baseRequest()
                .body(createAssetEntryDto())
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(400);
        verify(service, never()).create(any(), any());
    }

    @Test
    void createAsset_alreadyExists() {
        var asset = createAssetBuilder().build();
        when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(DataAddress.Builder.newInstance().type("any").build()));
        when(service.create(any(), any())).thenReturn(ServiceResult.conflict("already exists"));

        baseRequest()
                .body(createAssetEntryDto())
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(409);
    }

    @Test
    void createAsset_emptyAttributes() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.failure("Cannot be transformed"));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(DataAddress.Builder.newInstance().type("any").build()));
        var assetEntryDto = AssetEntryNewDto.Builder.newInstance()
                .asset(Json.createObjectBuilder().build())
                .dataAddress(DataAddressDto.Builder.newInstance().properties(Map.of("type", "any")).build())
                .build();

        baseRequest()
                .body(assetEntryDto)
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(400);
    }

    @Test
    void deleteAsset() {
        when(service.delete("assetId"))
                .thenReturn(ServiceResult.success(createAssetBuilder().build()));

        baseRequest()
                .contentType(JSON)
                .delete("/assets/assetId")
                .then()
                .statusCode(204);
        verify(service).delete("assetId");
    }

    @Test
    void deleteAsset_notExists() {
        when(service.delete(any())).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .contentType(JSON)
                .delete("/assets/not-existent-id")
                .then()
                .statusCode(404);
    }

    @Test
    void deleteAsset_conflicts() {
        when(service.delete(any())).thenReturn(ServiceResult.conflict("conflict"));

        baseRequest()
                .contentType(JSON)
                .delete("/assets/id")
                .then()
                .statusCode(409);
    }

    @Test
    void getAssetAddress() {
        when(dataAddressResolver.resolveForAsset("id"))
                .thenReturn(DataAddress.Builder.newInstance().type("any").build());
        var dataAddressDto = DataAddressDto.Builder.newInstance().properties(Map.of("key", "value")).build();
        when(transformerRegistry.transform(isA(DataAddress.class), eq(DataAddressDto.class)))
                .thenReturn(Result.success(dataAddressDto));

        baseRequest()
                .get("/assets/id/dataaddress")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("properties.size()", greaterThan(0));
        verify(transformerRegistry).transform(isA(DataAddress.class), eq(DataAddressDto.class));
    }

    @Test
    void getAssetAddress_notFound() {
        when(dataAddressResolver.resolveForAsset(any())).thenReturn(null);

        baseRequest()
                .get("/assets/not-existent-id/address")
                .then()
                .statusCode(404);
    }

    @Test
    void updateAsset_whenExists() {
        var assetEntry = AssetUpdateRequestDto.Builder.newInstance()
                .properties(Map.of("key1", "value1"))
                .build();
        var asset = Asset.Builder.newInstance().property("key1", "value1").build();
        when(transformerRegistry.transform(isA(AssetUpdateRequestWrapperDto.class), eq(Asset.class)))
                .thenReturn(Result.success(asset));
        when(service.update(any(Asset.class))).thenReturn(ServiceResult.success());

        baseRequest()
                .body(assetEntry)
                .contentType(JSON)
                .put("/assets/assetId")
                .then()
                .statusCode(204);
        verify(service).update(eq(asset));
    }

    @Test
    void updateAsset_shouldReturnNotFound_whenItDoesNotExists() {
        var assetEntry = AssetUpdateRequestDto.Builder.newInstance()
                .properties(Map.of("key1", "value1"))
                .build();
        var asset = Asset.Builder.newInstance().property("key1", "value1").build();
        when(transformerRegistry.transform(isA(AssetUpdateRequestWrapperDto.class), eq(Asset.class)))
                .thenReturn(Result.success(asset));
        when(service.update(any(Asset.class))).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .body(assetEntry)
                .contentType(JSON)
                .put("/assets/assetId")
                .then()
                .statusCode(404);
    }

    @Test
    void updateAsset_shouldReturnBadRequest_whenTransformFails() {
        var assetEntry = AssetUpdateRequestDto.Builder.newInstance()
                .properties(Map.of("key1", "value1"))
                .build();
        when(transformerRegistry.transform(isA(AssetUpdateRequestWrapperDto.class), eq(Asset.class)))
                .thenReturn(Result.failure("error"));

        baseRequest()
                .body(assetEntry)
                .contentType(JSON)
                .put("/assets/assetId")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    void updateDataAddress_whenAssetExists() {
        var dataAddressDto = DataAddressDto.Builder.newInstance()
                .properties(Map.of("type", "test-type"))
                .build();
        var dataAddress = DataAddress.Builder.newInstance().type("test-type").property("key1", "value1").build();
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class)))
                .thenReturn(Result.success(dataAddress));
        when(service.update(any(), any(DataAddress.class))).thenReturn(ServiceResult.success());

        baseRequest()
                .body(dataAddressDto)
                .contentType(JSON)
                .put("/assets/assetId/dataaddress")
                .then()
                .statusCode(204);
        verify(service).update(eq("assetId"), eq(dataAddress));
    }

    @Test
    void updateDataAddress_shouldReturnNotFound_whenItDoesNotExists() {
        var dataAddressDto = DataAddressDto.Builder.newInstance()
                .properties(Map.of("type", "test-type"))
                .build();
        var dataAddress = DataAddress.Builder.newInstance().type("test-type").property("key1", "value1").build();
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(dataAddress));
        when(service.update(any(), any(DataAddress.class))).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .body(dataAddressDto)
                .contentType(JSON)
                .put("/assets/assetId/dataaddress")
                .then()
                .statusCode(404);
    }

    @Test
    void updateDataAddress_shouldReturnBadRequest_whenTransformationFails() {
        var dataAddressDto = DataAddressDto.Builder.newInstance()
                .properties(Map.of("type", "test-type"))
                .build();
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class)))
                .thenReturn(Result.failure("error"));

        baseRequest()
                .body(dataAddressDto)
                .contentType(JSON)
                .put("/assets/assetId/dataaddress")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Override
    protected Object controller() {
        return new AssetNewApiController(service, dataAddressResolver, transformerRegistry, new TitaniumJsonLd(mock(Monitor.class)));
    }

    private AssetEntryNewDto createAssetEntryDto() {
        return AssetEntryNewDto.Builder.newInstance()
                .asset(createAssetJson().build())
                .dataAddress(DataAddressDto.Builder.newInstance().properties(Map.of("type", "any")).build())
                .build();
    }

    private JsonObjectBuilder createAssetJson() {
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, EDC_ASSET_TYPE)
                .add(ID, TEST_ASSET_ID)
                .add("properties", createPropertiesBuilder().build());
    }

    private JsonObjectBuilder createPropertiesBuilder() {
        return Json.createObjectBuilder()
                .add("name", TEST_ASSET_NAME)
                .add("description", TEST_ASSET_DESCRIPTION)
                .add("edc:version", TEST_ASSET_VERSION)
                .add("contenttype", TEST_ASSET_CONTENTTYPE);
    }

    private JsonObjectBuilder createContextBuilder() {
        return Json.createObjectBuilder()
                .add(VOCAB, EDC_NAMESPACE)
                .add(EDC_PREFIX, EDC_NAMESPACE);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v2")
                .when();
    }

    private Asset.Builder createAssetBuilder() {
        return Asset.Builder.newInstance()
                .name(TEST_ASSET_NAME)
                .id(TEST_ASSET_ID)
                .contentType(TEST_ASSET_CONTENTTYPE)
                .description(TEST_ASSET_DESCRIPTION)
                .version(TEST_ASSET_VERSION);
    }
}
