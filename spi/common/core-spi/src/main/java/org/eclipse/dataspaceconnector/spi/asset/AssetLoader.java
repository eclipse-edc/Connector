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

package org.eclipse.dataspaceconnector.spi.asset;

import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.AssetEntry;

public interface AssetLoader {

    default void accept(Asset asset, DataAddress dataAddress) {
        accept(new AssetEntry(asset, dataAddress));
    }

    void accept(AssetEntry item);

    /**
     * Deletes an asset.
     *
     * @param assetId Id of the asset to be deleted.
     * @return Deleted Asset or null if asset did not exist.
     * @throws EdcPersistenceException if something goes wrong.
     */
    Asset deleteById(String assetId);

}
