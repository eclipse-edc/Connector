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
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.mockito.Mockito.times;

public class PostgresqlAssetLoaderTest {
    private AssetLoader assetLoader;

    // mocks
    private Repository repository;

    @BeforeEach
    public void setup() {
        repository = Mockito.mock(Repository.class);
        assetLoader = new PostgresAssetLoader(repository);
    }

    @Test
    public void testAccept() throws SQLException {
        Asset asset = Asset.Builder.newInstance().build();
        DataAddress address = DataAddress.Builder.newInstance().type("foo").build();

        assetLoader.accept(asset, address);

        Mockito.verify(repository, times(1))
                .create(asset, address);
    }

    @Test
    public void testAcceptEntry() throws SQLException {
        Asset asset = Asset.Builder.newInstance().build();
        DataAddress address = DataAddress.Builder.newInstance().type("foo").build();

        assetLoader.accept(new AssetEntry(asset, address));

        Mockito.verify(repository, times(1))
                .create(asset, address);
    }
}
