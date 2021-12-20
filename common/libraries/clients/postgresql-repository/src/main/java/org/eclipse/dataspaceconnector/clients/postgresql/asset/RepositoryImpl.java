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

package org.eclipse.dataspaceconnector.clients.postgresql.asset;

import org.eclipse.dataspaceconnector.clients.postgresql.PostgresqlClient;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.operation.AddressQueryOperation;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.operation.AddressUpdateOperation;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.operation.AssetAndAddressCreateOperation;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.operation.AssetDeleteOperation;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.operation.AssetQueryOperation;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.operation.AssetUpdateOperation;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class RepositoryImpl implements Repository {

    private final AssetAndAddressCreateOperation assetAndAddressCreateOperation;
    private final AssetUpdateOperation assetUpdateOperation;
    private final AssetDeleteOperation assetDeleteOperation;
    private final AssetQueryOperation assetQueryOperation;
    private final AddressUpdateOperation addressUpdateOperation;
    private final AddressQueryOperation addressQueryOperation;

    public RepositoryImpl(@NotNull PostgresqlClient postgresqlClient) {
        Objects.requireNonNull(postgresqlClient);
        assetAndAddressCreateOperation = new AssetAndAddressCreateOperation(postgresqlClient);
        assetUpdateOperation = new AssetUpdateOperation(postgresqlClient);
        assetDeleteOperation = new AssetDeleteOperation(postgresqlClient);
        assetQueryOperation = new AssetQueryOperation(postgresqlClient);
        addressUpdateOperation = new AddressUpdateOperation(postgresqlClient);
        addressQueryOperation = new AddressQueryOperation(postgresqlClient);
    }

    @NotNull
    @Override
    public List<Asset> queryAssets(@NotNull List<Criterion> criteria) throws SQLException {
        return assetQueryOperation.invoke(Objects.requireNonNull(criteria));
    }

    @Override
    public @NotNull List<DataAddress> queryAddress(@NotNull List<Criterion> criteria) throws SQLException {
        return addressQueryOperation.invoke(criteria);
    }

    @Override
    public void create(@NotNull Asset asset, @NotNull DataAddress dataAddress) throws SQLException {
        assetAndAddressCreateOperation.invoke(
                Objects.requireNonNull(asset),
                Objects.requireNonNull(dataAddress));
    }

    @Override
    public void update(@NotNull Asset asset) throws SQLException {
        assetUpdateOperation.invoke(Objects.requireNonNull(asset));
    }

    @Override
    public void update(@NotNull Asset asset, @NotNull DataAddress dataAddress) throws SQLException {
        addressUpdateOperation.invoke(asset, dataAddress);
    }

    @Override
    public void delete(@NotNull Asset asset) throws SQLException {
        assetDeleteOperation.invoke(Objects.requireNonNull(asset));
    }
}
