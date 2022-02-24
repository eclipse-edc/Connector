/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.DataAddressDto;

import java.util.Collections;

public class TestFunctions {

    static AssetEntryDto createAssetEntryDto() {
        var assetDto = AssetDto.Builder.newInstance().properties(Collections.singletonMap("Asset-1", "An Asset")).build();
        var dataAddress = DataAddressDto.Builder.newInstance().properties(Collections.singletonMap("asset-1", "/localhost")).build();
        return AssetEntryDto.Builder.newInstance().assetDto(assetDto).dataAddress(dataAddress).build();
    }

    static AssetEntryDto createAssetEntryDto_emptyAttributes() {
        var assetDto = AssetDto.Builder.newInstance().properties(Collections.singletonMap("", "")).build();
        var dataAddress = DataAddressDto.Builder.newInstance().properties(Collections.singletonMap("", "")).build();
        return AssetEntryDto.Builder.newInstance().assetDto(assetDto).dataAddress(dataAddress).build();
    }
}
