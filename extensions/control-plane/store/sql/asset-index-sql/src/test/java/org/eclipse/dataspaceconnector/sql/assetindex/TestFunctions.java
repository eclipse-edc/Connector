/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.sql.assetindex;

import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

public class TestFunctions {

    public static Asset createAsset(String assetId) {
        return createAssetBuilder(assetId)
                .build();
    }

    public static Asset.Builder createAssetBuilder(String assetId) {
        return Asset.Builder.newInstance()
                .id(assetId)
                .name("test-asset")
                .version("0.0.1-test");
    }

    public static DataAddress createDataAddress(String type) {
        return createDataAddressBuilder(type).build();
    }

    public static DataAddress.Builder createDataAddressBuilder(String type) {
        return DataAddress.Builder.newInstance()
                .type(type);
    }
}
