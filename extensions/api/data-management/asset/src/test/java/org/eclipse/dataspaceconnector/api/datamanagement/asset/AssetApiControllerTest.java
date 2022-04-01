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

import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.DataAddressDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetService;
import org.eclipse.dataspaceconnector.api.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
                .asset(AssetDto.Builder.newInstance().build())
                .dataAddress(DataAddressDto.Builder.newInstance().build())
                .build();
        var asset = Asset.Builder.newInstance().build();
        when(transformerRegistry.transform(isA(AssetDto.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(DataAddress.Builder.newInstance().type("any").build()));
        when(service.create(any(), any())).thenReturn(ServiceResult.success(asset));

        controller.createAsset(assetEntry);

        verify(transformerRegistry).transform(any(), eq(Asset.class));
        verify(transformerRegistry).transform(any(), eq(DataAddress.class));
        verify(service).create(isA(Asset.class), isA(DataAddress.class));
    }

    @Test
    void createAsset_alreadyExists() {
        var assetEntry = AssetEntryDto.Builder.newInstance()
                .asset(AssetDto.Builder.newInstance().build())
                .dataAddress(DataAddressDto.Builder.newInstance().build())
                .build();
        var asset = Asset.Builder.newInstance().build();
        when(transformerRegistry.transform(isA(AssetDto.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.success(DataAddress.Builder.newInstance().type("any").build()));
        when(service.create(any(), any())).thenReturn(ServiceResult.conflict("already exists"));

        assertThatThrownBy(() -> controller.createAsset(assetEntry)).isInstanceOf(ObjectExistsException.class);
    }

    @Test
    void createAsset_transformFails() {
        var assetEntry = AssetEntryDto.Builder.newInstance()
                .asset(AssetDto.Builder.newInstance().build())
                .dataAddress(DataAddressDto.Builder.newInstance().build())
                .build();
        when(transformerRegistry.transform(isA(AssetDto.class), eq(Asset.class))).thenReturn(Result.failure("failed"));
        when(transformerRegistry.transform(isA(DataAddressDto.class), eq(DataAddress.class))).thenReturn(Result.failure("failed"));
        when(service.create(any(), any())).thenReturn(ServiceResult.conflict("already exists"));

        assertThatThrownBy(() -> controller.createAsset(assetEntry)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAllAssets() {
        when(service.query(any())).thenReturn(List.of(Asset.Builder.newInstance().build()));
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetDto.class))).thenReturn(Result.success(AssetDto.Builder.newInstance().build()));

        var allAssets = controller.getAllAssets(1, 10, "field=value", SortOrder.ASC, "field");

        assertThat(allAssets).hasSize(1);
        verify(service).query(argThat(s ->
                s.getOffset() == 1 &&
                s.getLimit() == 10 &&
                s.getFilterExpression().size() == 1 &&
                s.getFilterExpression().get(0).equals(new Criterion("field", "=", "value")) &&
                s.getSortOrder().equals(SortOrder.ASC) &&
                s.getSortField().equals("field")
        ));
        verify(transformerRegistry).transform(isA(Asset.class), eq(AssetDto.class));
    }

    @Test
    void getAllAssets_filtersOutFailedTransforms() {
        when(service.query(any())).thenReturn(List.of(Asset.Builder.newInstance().build()));
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetDto.class))).thenReturn(Result.failure("failed to transform"));

        var allAssets = controller.getAllAssets(1, 10, "field=value", SortOrder.ASC, "field");

        assertThat(allAssets).isEmpty();
    }

    @Test
    void getAssetById() {
        when(service.findById("id")).thenReturn(Asset.Builder.newInstance().build());
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetDto.class))).thenReturn(Result.success(AssetDto.Builder.newInstance().build()));

        var assetDto = controller.getAsset("id");

        assertThat(assetDto).isNotNull();
        verify(transformerRegistry).transform(isA(Asset.class), eq(AssetDto.class));
    }

    @Test
    void getAssetById_notExists() {
        when(service.findById("id")).thenReturn(null);

        assertThatThrownBy(() -> controller.getAsset("id")).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void getAssetById_notExistsIfTransformFails() {
        when(service.findById("id")).thenReturn(Asset.Builder.newInstance().build());
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetDto.class))).thenReturn(Result.failure("failure"));

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
