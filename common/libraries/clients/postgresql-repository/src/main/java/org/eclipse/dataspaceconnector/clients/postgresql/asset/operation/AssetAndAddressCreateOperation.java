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

package org.eclipse.dataspaceconnector.clients.postgresql.asset.operation;

import org.eclipse.dataspaceconnector.clients.postgresql.PostgresqlClient;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.serializer.EnvelopePacker;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.util.PreparedStatementResourceReader;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Objects;

public class AssetAndAddressCreateOperation {
    private final PostgresqlClient postgresClient;

    public AssetAndAddressCreateOperation(@NotNull PostgresqlClient postgresqlClient) {
        this.postgresClient = Objects.requireNonNull(postgresqlClient);
    }

    public void invoke(@NotNull Asset asset, @NotNull DataAddress dataAddress) throws SQLException {
        Objects.requireNonNull(asset);
        Objects.requireNonNull(dataAddress);

        String sqlCreateAsset = PreparedStatementResourceReader.readAssetCreate();
        String sqlCreateAddress = PreparedStatementResourceReader.readAddressCreate();
        String sqlCreateAssetProperty = PreparedStatementResourceReader.readAssetPropertyCreate();
        String sqlCreateAddressProperty = PreparedStatementResourceReader.readAddressPropertyCreate();

        postgresClient.doInTransaction(client -> {
            client.execute(sqlCreateAsset, asset.getId());

            for (var entrySet : asset.getProperties().entrySet()) {
                String id = asset.getId();
                String key = entrySet.getKey();
                String value = EnvelopePacker.pack(entrySet.getValue());

                client.execute(sqlCreateAssetProperty, id, key, value);
            }

            client.execute(sqlCreateAddress, asset.getId());

            for (var entrySet : dataAddress.getProperties().entrySet()) {
                String id = asset.getId();
                String key = entrySet.getKey();
                String value = EnvelopePacker.pack(entrySet.getValue());

                client.execute(sqlCreateAddressProperty, id, key, value);
            }
        });
    }
}
