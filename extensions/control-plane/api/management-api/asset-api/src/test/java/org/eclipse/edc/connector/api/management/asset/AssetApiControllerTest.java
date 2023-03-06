/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *       Microsoft Corporation - Added initiate-transfer endpoint tests
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Improvements
 *
 */

package org.eclipse.edc.connector.api.management.asset;


import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.management.asset.model.AssetCreationRequestDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetResponseDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetUpdateRequestDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetUpdateRequestWrapperDto;
import org.eclipse.edc.connector.api.management.asset.model.DataAddressDto;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AssetApiControllerTest {

    private final AssetService service = mock(AssetService.class);

    private final DataAddressResolver dataAddressResolver = mock(DataAddressResolver.class);
    private final DtoTransformerRegistry transformerRegistry = mock(DtoTransformerRegistry.class);
    private AssetApiController controller;

    @BeforeEach
    void setUp() {
        var monitor = mock(Monitor.class);
        controller = new AssetApiController(monitor, service, dataAddressResolver, transformerRegistry);
    }

    @Test
    void createAsset() {
        var assetEntry = AssetEntryDto.Builder.newInstance()
                .asset(AssetCreationRequestDto.Builder.newInstance().build())
                .dataAddress(DataAddressDto.Builder.newInstance().build())
                .build();
        var asset = Asset.Builder.newInstance().build();
        when(transformerRegistry.transform(isA(AssetCreationRequestDto.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(DataAddress.Builder.newInstance().type("any").build()));
        when(service.create(any(), any())).thenReturn(ServiceResult.success(asset));

        var assetId = controller.createAsset(assetEntry);

        assertThat(assetId).isNotNull();
        assertThat(assetId.getId()).isNotEmpty();
        assertThat(assetId).isInstanceOf(IdResponseDto.class);
        assertThat(assetId.getCreatedAt()).isNotEqualTo(0L);

        verify(transformerRegistry).transform(any(), eq(Asset.class));
        verify(transformerRegistry).transform(any(), eq(DataAddress.class));
        verify(service).create(isA(Asset.class), isA(DataAddress.class));
    }

    @Test
    void createAsset_returnExpectedId() {
        var assetId = UUID.randomUUID().toString();
        var assetEntry = AssetEntryDto.Builder.newInstance()
                .asset(AssetCreationRequestDto.Builder.newInstance().build())
                .dataAddress(DataAddressDto.Builder.newInstance().build())
                .build();

        var asset = Asset.Builder.newInstance().id(assetId).build();
        when(transformerRegistry.transform(isA(AssetCreationRequestDto.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(DataAddress.Builder.newInstance().type("any").build()));
        when(service.create(any(), any())).thenReturn(ServiceResult.success(asset));

        var returnedAssetId = controller.createAsset(assetEntry);
        assertThat(returnedAssetId).isNotNull();
        assertThat(returnedAssetId.getId()).isEqualTo(assetId);
    }

    @Test
    void createAsset_alreadyExists() {
        var assetEntry = AssetEntryDto.Builder.newInstance()
                .asset(AssetCreationRequestDto.Builder.newInstance().build())
                .dataAddress(DataAddressDto.Builder.newInstance().build())
                .build();
        var asset = Asset.Builder.newInstance().build();
        when(transformerRegistry.transform(isA(AssetCreationRequestDto.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(DataAddress.Builder.newInstance().type("any").build()));
        when(service.create(any(), any())).thenReturn(ServiceResult.conflict("already exists"));

        assertThatThrownBy(() -> controller.createAsset(assetEntry)).isInstanceOf(ObjectConflictException.class);
    }

    @Test
    void createAsset_transformFails() {
        var assetEntry = AssetEntryDto.Builder.newInstance()
                .asset(AssetCreationRequestDto.Builder.newInstance().build())
                .dataAddress(DataAddressDto.Builder.newInstance().build())
                .build();
        when(transformerRegistry.transform(isA(AssetCreationRequestDto.class), eq(Asset.class))).thenReturn(Result.failure("failed"));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.failure("failed"));
        when(service.create(any(), any())).thenReturn(ServiceResult.conflict("already exists"));

        assertThatThrownBy(() -> controller.createAsset(assetEntry)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getAllAssets() {
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(Asset.Builder.newInstance().build())));
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetResponseDto.class)))
                .thenReturn(Result.success(AssetResponseDto.Builder.newInstance().build()));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var querySpec = QuerySpecDto.Builder.newInstance().build();

        var allAssets = controller.getAllAssets(querySpec);

        assertThat(allAssets).hasSize(1);
        verify(service).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(isA(Asset.class), eq(AssetResponseDto.class));
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void getAll_filtersOutFailedTransforms() {
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(Asset.Builder.newInstance().build())));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetResponseDto.class))).thenReturn(Result.failure("failed to transform"));

        var allAssets = controller.getAllAssets(QuerySpecDto.Builder.newInstance().build());

        assertThat(allAssets).isEmpty();
    }

    @Test
    void getAll_throwsExceptionIfQuerySpecTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("Cannot transform"));

        assertThatThrownBy(() -> controller.getAllAssets(QuerySpecDto.Builder.newInstance().build())).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getAll_throwsExceptionIfQueryFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.none()));

        when(service.query(any())).thenReturn(ServiceResult.badRequest("error"));

        assertThatThrownBy(() -> controller.getAllAssets(QuerySpecDto.Builder.newInstance().build())).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void queryAllAssets() {
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(Asset.Builder.newInstance().build())));
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetResponseDto.class)))
                .thenReturn(Result.success(AssetResponseDto.Builder.newInstance().build()));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var querySpec = QuerySpecDto.Builder.newInstance().build();

        var allAssets = controller.requestAssets(querySpec);

        assertThat(allAssets).hasSize(1);
        verify(service).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(isA(Asset.class), eq(AssetResponseDto.class));
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void queryAll_filtersOutFailedTransforms() {
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(Asset.Builder.newInstance().build())));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetResponseDto.class))).thenReturn(Result.failure("failed to transform"));

        var allAssets = controller.requestAssets(QuerySpecDto.Builder.newInstance().build());

        assertThat(allAssets).isEmpty();
    }

    @Test
    void queryAll_throwsExceptionIfQuerySpecTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("Cannot transform"));

        assertThatThrownBy(() -> controller.requestAssets(QuerySpecDto.Builder.newInstance().build())).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void queryAll_throwsExceptionIfQueryFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.none()));

        when(service.query(any())).thenReturn(ServiceResult.badRequest("error"));

        assertThatThrownBy(() -> controller.getAllAssets(QuerySpecDto.Builder.newInstance().build())).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getAssetById() {
        when(service.findById("id")).thenReturn(Asset.Builder.newInstance().build());
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetResponseDto.class))).thenReturn(Result.success(AssetResponseDto.Builder.newInstance().build()));

        var assetDto = controller.getAsset("id");

        assertThat(assetDto).isNotNull();
        verify(transformerRegistry).transform(isA(Asset.class), eq(AssetResponseDto.class));
    }

    @Test
    void getAssetById_notExists() {
        when(service.findById("id")).thenReturn(null);

        assertThatThrownBy(() -> controller.getAsset("id")).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void getAssetById_notExistsIfTransformFails() {
        when(service.findById("id")).thenReturn(Asset.Builder.newInstance().build());
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetResponseDto.class))).thenReturn(Result.failure("failure"));

        assertThatThrownBy(() -> controller.getAsset("id")).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void deleteAsset() {
        when(service.delete("id")).thenReturn(ServiceResult.success(Asset.Builder.newInstance().build()));

        controller.removeAsset("id");

        verify(service).delete("id");
    }

    @Test
    void deleteAsset_notExists() {
        when(service.delete("id")).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller.removeAsset("id")).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void deleteAsset_conflicts() {
        when(service.delete("id")).thenReturn(ServiceResult.conflict("conflicting"));

        assertThatThrownBy(() -> controller.removeAsset("id")).isInstanceOf(ObjectConflictException.class);
    }

    @Test
    void getDataAddressForAssetById() {

        when(dataAddressResolver.resolveForAsset("id")).thenReturn(DataAddress.Builder.newInstance().type("any").build());
        when(transformerRegistry.transform(isA(DataAddress.class), eq(DataAddressDto.class))).thenReturn(Result.success(DataAddressDto.Builder.newInstance().build()));

        var assetDto = controller.getAssetDataAddress("id");

        assertThat(assetDto).isNotNull();
        verify(transformerRegistry).transform(isA(DataAddress.class), eq(DataAddressDto.class));
    }

    @Test
    void update_whenExists() {
        var assetEntry = AssetUpdateRequestDto.Builder.newInstance()
                .properties(Map.of("key1", "value1"))
                .build();
        var asset = Asset.Builder.newInstance().property("key1", "value1").build();
        when(transformerRegistry.transform(isA(AssetUpdateRequestWrapperDto.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(service.update(any(Asset.class))).thenReturn(ServiceResult.success());

        var assetId = "test-asset-1";
        controller.updateAsset(assetId, assetEntry);
        verify(service).update(eq(asset));
        assertThatNoException();
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        var assetEntry = AssetUpdateRequestDto.Builder.newInstance()
                .properties(Map.of("key1", "value1"))
                .build();
        var asset = Asset.Builder.newInstance().property("key1", "value1").build();
        when(transformerRegistry.transform(isA(AssetUpdateRequestWrapperDto.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(service.update(any(Asset.class))).thenReturn(ServiceResult.notFound("not found"));

        var assetId = "test-asset-1";
        assertThatThrownBy(() -> controller.updateAsset(assetId, assetEntry)).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void update_whenTransformationFails_shouldThrowException() {
        var assetEntry = AssetUpdateRequestDto.Builder.newInstance()
                .properties(Map.of("key1", "value1"))
                .build();
        when(transformerRegistry.transform(isA(AssetUpdateRequestWrapperDto.class), eq(Asset.class))).thenReturn(Result.failure("test"));

        var assetId = "test-asset-1";
        assertThatThrownBy(() -> controller.updateAsset(assetId, assetEntry))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void updateDataAddress_whenExists() {
        var dataAddressDto = DataAddressDto.Builder.newInstance()
                .properties(Map.of("key1", "value1"))
                .build();
        var dataAddress = DataAddress.Builder.newInstance().type("test-type").property("key1", "value1").build();
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(dataAddress));
        when(service.update(any(), any(DataAddress.class))).thenReturn(ServiceResult.success());

        var assetId = "test-dataAddress-1";
        controller.updateDataAddress(assetId, dataAddressDto);
        verify(service).update(eq(assetId), eq(dataAddress));
        assertThatNoException();
    }

    @Test
    void updateDataAddress_whenTransformationFails_shouldThrowException() {
        var dataAddressDto = DataAddressDto.Builder.newInstance()
                .properties(Map.of("key1", "value1"))
                .build();
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.failure("test"));

        var assetId = "test-dataAddress-1";
        assertThatThrownBy(() -> controller.updateDataAddress(assetId, dataAddressDto))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void updateDataAddress_whenNotExists_shouldThrowException() {
        var dataAddressDto = DataAddressDto.Builder.newInstance()
                .properties(Map.of("key1", "value1"))
                .build();
        var dataAddress = DataAddress.Builder.newInstance().type("test-type").property("key1", "value1").build();
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(dataAddress));
        when(service.update(any(), any(DataAddress.class))).thenReturn(ServiceResult.notFound("not found"));

        var assetId = "test-asset-1";
        assertThatThrownBy(() -> controller.updateDataAddress(assetId, dataAddressDto)).isInstanceOf(ObjectNotFoundException.class);
    }
}
