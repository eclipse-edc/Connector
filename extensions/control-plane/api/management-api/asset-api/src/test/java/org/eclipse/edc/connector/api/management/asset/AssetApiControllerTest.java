/*
 *  Copyright (c) 2022 - 2023 ZF Friedrichshafen AG
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
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetCreationRequestDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetResponseDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetUpdateRequestDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetUpdateRequestWrapperDto;
import org.eclipse.edc.connector.api.management.asset.model.DataAddressDto;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
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
public class AssetApiControllerTest extends RestControllerTestBase {

    private final AssetService service = mock(AssetService.class);
    private final DataAddressResolver dataAddressResolver = mock(DataAddressResolver.class);
    private final TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);

    @Test
    void getAllAssets() {
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(Asset.Builder.newInstance().build())));
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetResponseDto.class)))
                .thenReturn(Result.success(AssetResponseDto.Builder.newInstance().build()));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));

        baseRequest()
                .get("/assets")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
        verify(service).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(isA(Asset.class), eq(AssetResponseDto.class));
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void getAll_filtersOutFailedTransforms() {
        when(service.query(any()))
                .thenReturn(ServiceResult.success(Stream.of(Asset.Builder.newInstance().build())));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetResponseDto.class)))
                .thenReturn(Result.failure("failed to transform"));

        baseRequest()
                .get("/assets")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(0));
    }

    @Test
    void getAll_invalidQuery() {
        baseRequest()
                .get("/assets?limit=0&offset=-1&filter=&sortField=")
                .then()
                .statusCode(400);
    }

    @Test
    void getAll_shouldReturnBadRequest_whenServiceReturnsBadRequest() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.none()));
        when(service.query(any())).thenReturn(ServiceResult.badRequest("error"));

        baseRequest()
                .get("/assets")
                .then()
                .statusCode(400);
    }

    @Test
    void queryAllAssets() {
        when(service.query(any()))
                .thenReturn(ServiceResult.success(Stream.of(Asset.Builder.newInstance().build())));
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetResponseDto.class)))
                .thenReturn(Result.success(AssetResponseDto.Builder.newInstance().build()));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));

        baseRequest()
                .contentType(JSON)
                .post("/assets/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
        verify(service).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(isA(Asset.class), eq(AssetResponseDto.class));
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void queryAll_filtersOutFailedTransforms() {
        when(service.query(any()))
                .thenReturn(ServiceResult.success(Stream.of(Asset.Builder.newInstance().build())));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetResponseDto.class)))
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
    void queryAll_shouldReturnBadRequest_whenQueryIsInvalid() {
        baseRequest()
                .body(Map.of("offset", -1))
                .contentType(JSON)
                .post("/assets/request")
                .then()
                .statusCode(400);
    }

    @Test
    void getAll_shouldReturnBadRequest_whenQueryTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("error"));
        when(service.query(any())).thenReturn(ServiceResult.success());

        baseRequest()
                .get("/assets")
                .then()
                .statusCode(400);
    }

    @Test
    void queryAll_shouldReturnBadRequest_whenServiceReturnsBadRequest() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
        when(service.query(any())).thenReturn(ServiceResult.badRequest());

        baseRequest()
                .get("/assets")
                .then()
                .statusCode(400);
    }

    @Test
    void getSingleAsset() {
        var asset = Asset.Builder.newInstance().property("key", "value").build();
        when(service.findById("id")).thenReturn(asset);
        var response = AssetResponseDto.Builder.newInstance().properties(Map.of("key", "value")).build();
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetResponseDto.class))).thenReturn(Result.success(response));

        baseRequest()
                .get("/assets/id")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("properties.size()", is(1));
        verify(transformerRegistry).transform(isA(Asset.class), eq(AssetResponseDto.class));
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
    void postAsset() {
        var assetEntry = createAssetEntryDto();
        var asset = Asset.Builder.newInstance().id("assetId").build();
        when(transformerRegistry.transform(isA(AssetCreationRequestDto.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(DataAddress.Builder.newInstance().type("any").build()));
        when(service.create(any(), any())).thenReturn(ServiceResult.success(asset));

        baseRequest()
                .body(assetEntry)
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", is("assetId"))
                .body("createdAt", greaterThan(0L));

        verify(transformerRegistry).transform(any(), eq(Asset.class));
        verify(transformerRegistry).transform(any(), eq(DataAddress.class));
        verify(service).create(isA(Asset.class), isA(DataAddress.class));
    }

    @Test
    void postAsset_shouldReturnBadRequest_whenDataAddressIsNull() {
        var assetDto = AssetCreationRequestDto.Builder.newInstance().id("assetId").properties(Map.of("Asset-1", "An Asset")).build();
        var assetEntryDto = AssetEntryDto.Builder.newInstance().asset(assetDto).dataAddress(null).build();

        baseRequest()
                .body(assetEntryDto)
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    void postAsset_shouldReturnBadRequest_whenTransformFails() {
        when(transformerRegistry.transform(isA(AssetCreationRequestDto.class), eq(Asset.class))).thenReturn(Result.failure("failed"));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.failure("failed"));
        when(service.create(any(), any())).thenReturn(ServiceResult.conflict("already exists"));

        baseRequest()
                .body(createAssetEntryDto())
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(400);
        verify(service, never()).create(any(), any());
    }

    @Test
    void postAsset_alreadyExists() {
        var asset = Asset.Builder.newInstance().build();
        when(transformerRegistry.transform(isA(AssetCreationRequestDto.class), eq(Asset.class))).thenReturn(Result.success(asset));
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
    void deleteAsset() {
        when(service.delete("assetId"))
                .thenReturn(ServiceResult.success(Asset.Builder.newInstance().build()));

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
                .get("/assets/id/address")
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
        return new AssetApiController(monitor, service, dataAddressResolver, transformerRegistry);
    }

    private AssetEntryDto createAssetEntryDto() {
        return AssetEntryDto.Builder.newInstance()
                .asset(AssetCreationRequestDto.Builder.newInstance().properties(Map.of("key", "value")).build())
                .dataAddress(DataAddressDto.Builder.newInstance().properties(Map.of("type", "any")).build())
                .build();
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }

    private AssetEntryDto createAssetEntryDto_emptyAttributes() {
        var assetDto = AssetCreationRequestDto.Builder.newInstance().properties(Collections.singletonMap("", "")).build();
        var dataAddress = DataAddressDto.Builder.newInstance().properties(Collections.singletonMap("", "")).build();
        return AssetEntryDto.Builder.newInstance().asset(assetDto).dataAddress(dataAddress).build();
    }
}
