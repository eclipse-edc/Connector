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
import org.eclipse.dataspaceconnector.clients.postgresql.asset.util.Sql;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Objects;

public class CreateOperation {
    private final PostgresqlClient postgresClient;

    public CreateOperation(@NotNull PostgresqlClient postgresqlClient) {
        this.postgresClient = Objects.requireNonNull(postgresqlClient);
    }

    public void invoke(@NotNull Asset asset) throws SQLException {
        Objects.requireNonNull(asset);

        String sqlCreateAsset = Sql.getAssetCreate();
        String sqlCreateProperty = Sql.getPropertyCreate();

        postgresClient.doInTransaction(client -> {
            client.execute(sqlCreateAsset, asset.getId());

            for (var entrySet : asset.getProperties().entrySet()) {
                String id = asset.getId();
                String key = entrySet.getKey();
                String value = EnvelopePacker.pack(entrySet.getValue());

                client.execute(sqlCreateProperty, id, key, value);
            }
        });
    }
}
