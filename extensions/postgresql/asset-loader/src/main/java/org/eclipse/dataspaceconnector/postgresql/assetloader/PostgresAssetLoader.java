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

package org.eclipse.dataspaceconnector.postgresql.assetloader;

import org.eclipse.dataspaceconnector.clients.postgresql.asset.Repository;
import org.eclipse.dataspaceconnector.dataloading.AssetEntry;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Objects;

public class PostgresAssetLoader implements AssetLoader {

    private final Repository assetRepository;

    public PostgresAssetLoader(@NotNull Repository assetRepository) {
        this.assetRepository = Objects.requireNonNull(assetRepository);
    }

    @Override
    public void accept(Asset asset, DataAddress dataAddress) {
        try {
            assetRepository.create(asset, dataAddress);
        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void accept(AssetEntry item) {
        accept(item.getAsset(), item.getDataAddress());
    }
}
