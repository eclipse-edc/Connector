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

package org.eclipse.dataspaceconnector.api.datamanagement.asset;


import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetResponseDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.DataAddressDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetService;
import org.eclipse.dataspaceconnector.api.model.IdResponseDto;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.exception.InvalidRequestException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    private final DtoTransformerRegistry transformerRegistry = mock(DtoTransformerRegistry.class);
    private AssetApiController controller;

    @BeforeEach
    void setUp() {
        var monitor = mock(Monitor.class);
        controller = new AssetApiController(monitor, service, transformerRegistry);
    }

    @Test
    void createAsset() {
        var assetEntry = AssetEntryDto.Builder.newInstance()
                .asset(AssetRequestDto.Builder.newInstance().build())
                .dataAddress(DataAddressDto.Builder.newInstance().build())
                .build();
        var asset = Asset.Builder.newInstance().build();
        when(transformerRegistry.transform(isA(AssetRequestDto.class), eq(Asset.class))).thenReturn(Result.success(asset));
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
                .asset(AssetRequestDto.Builder.newInstance().build())
                .dataAddress(DataAddressDto.Builder.newInstance().build())
                .build();

        var asset = Asset.Builder.newInstance().id(assetId).build();
        when(transformerRegistry.transform(isA(AssetRequestDto.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(DataAddress.Builder.newInstance().type("any").build()));
        when(service.create(any(), any())).thenReturn(ServiceResult.success(asset));

        var returnedAssetId = controller.createAsset(assetEntry);
        assertThat(returnedAssetId).isNotNull();
        assertThat(returnedAssetId.getId()).isEqualTo(assetId);
    }

    @Test
    void createAsset_alreadyExists() {
        var assetEntry = AssetEntryDto.Builder.newInstance()
                .asset(AssetRequestDto.Builder.newInstance().build())
                .dataAddress(DataAddressDto.Builder.newInstance().build())
                .build();
        var asset = Asset.Builder.newInstance().build();
        when(transformerRegistry.transform(isA(AssetRequestDto.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(DataAddress.Builder.newInstance().type("any").build()));
        when(service.create(any(), any())).thenReturn(ServiceResult.conflict("already exists"));

        assertThatThrownBy(() -> controller.createAsset(assetEntry)).isInstanceOf(ObjectExistsException.class);
    }

    @Test
    void createAsset_transformFails() {
        var assetEntry = AssetEntryDto.Builder.newInstance()
                .asset(AssetRequestDto.Builder.newInstance().build())
                .dataAddress(DataAddressDto.Builder.newInstance().build())
                .build();
        when(transformerRegistry.transform(isA(AssetRequestDto.class), eq(Asset.class))).thenReturn(Result.failure("failed"));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.failure("failed"));
        when(service.create(any(), any())).thenReturn(ServiceResult.conflict("already exists"));

        assertThatThrownBy(() -> controller.createAsset(assetEntry)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getAllAssets() {
        when(service.query(any())).thenReturn(ServiceResult.success(List.of(Asset.Builder.newInstance().build())));
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
        when(service.query(any())).thenReturn(ServiceResult.success(List.of(Asset.Builder.newInstance().build())));
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

        assertThatThrownBy(() -> controller.removeAsset("id")).isInstanceOf(ObjectExistsException.class);
    }

}
