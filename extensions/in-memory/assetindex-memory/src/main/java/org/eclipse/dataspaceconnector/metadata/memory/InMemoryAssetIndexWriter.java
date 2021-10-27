/*
 *  Copyright (c) 2021 BMW Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       BMW Group - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndexWriter;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

public class InMemoryAssetIndexWriter implements AssetIndexWriter {

    private final InMemoryAssetIndex assetIndex;

    public InMemoryAssetIndexWriter(InMemoryAssetIndex assetIndex) {
        this.assetIndex = assetIndex;
    }

    @Override
    public void add(Asset asset) {
        assetIndex.add(asset, null);
    }
}
