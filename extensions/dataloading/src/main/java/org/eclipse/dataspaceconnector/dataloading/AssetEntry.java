/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.dataloading;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

public class AssetEntry {
    private final Asset asset;
    private final DataAddress dataAddress;

    @JsonCreator
    public AssetEntry(@JsonProperty("asset") Asset asset, @JsonProperty("dataAddress") DataAddress dataAddress) {
        this.asset = asset;
        this.dataAddress = dataAddress;
    }

    public Asset getAsset() {
        return asset;
    }

    public DataAddress getDataAddress() {
        return dataAddress;

    }
}
