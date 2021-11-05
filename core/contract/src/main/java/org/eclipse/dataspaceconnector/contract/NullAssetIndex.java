/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndexQuery;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndexResult;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Collections;

/**
 * NullObject of the {@link AssetIndex}
 */
public class NullAssetIndex implements AssetIndex {

    @Override
    public AssetIndexResult queryAssets(AssetIndexQuery query) {
        return AssetIndexResult.Builder.newInstance().expression(query.getExpression()).assets(Collections.emptyList()).build();
    }

    @Override
    public Asset findById(String assetId) {
        return null;
    }
}
