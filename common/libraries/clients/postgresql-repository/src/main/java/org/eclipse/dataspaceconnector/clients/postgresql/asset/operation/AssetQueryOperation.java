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
import org.eclipse.dataspaceconnector.clients.postgresql.asset.mapper.IdMapper;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.mapper.PropertyMapper;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.serializer.EnvelopePacker;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.types.Property;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.util.PreparedStatementResourceReader;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AssetQueryOperation {

    private final PostgresqlClient postgresqlClient;

    public AssetQueryOperation(@NotNull PostgresqlClient postgresClient) {
        this.postgresqlClient = Objects.requireNonNull(postgresClient);
    }

    @NotNull
    public List<Asset> invoke(@NotNull List<Criterion> criteria) throws SQLException {
        Objects.requireNonNull(criteria);
        throwOnNotSupported(criteria);

        if (criteria.isEmpty()) {
            return Collections.emptyList();
        }

        String sqlQueryTemplate = PreparedStatementResourceReader.readAssetQuery();
        String sqlPropertiesByKv = PreparedStatementResourceReader.readAssetPropertiesSelectByKv();
        String sqlPropertiesByAssetId = PreparedStatementResourceReader.readAssetPropertiesSelectByAssetId();
        String sqlAssetsAll = PreparedStatementResourceReader.readAssetSelectAll();

        List<String> targetAssetIds;
        if (isSelectAll(criteria)) {
            targetAssetIds = postgresqlClient.execute(new IdMapper(), sqlAssetsAll);
        } else {
            StringBuilder sqlQuery = new StringBuilder(sqlQueryTemplate);
            // start with 1, because the query template already contains 1 WHERE clause
            for (int i = 1; i < criteria.size(); i++) {
                sqlQuery.append(" AND asset_id IN ( ").append(sqlPropertiesByKv).append(" )");
            }

            List<Object> arguments = new ArrayList<>();
            criteria.forEach(c -> {
                        arguments.add(c.getOperandLeft());
                        arguments.add(EnvelopePacker.pack(c.getOperandRight()));
                    }
            );

            targetAssetIds = postgresqlClient.execute(new IdMapper(), sqlQuery.toString(), arguments.toArray());
        }

        List<Asset> assets = new ArrayList<>();

        for (String assetId : targetAssetIds) {
            List<Property> properties = postgresqlClient.execute(new PropertyMapper(), sqlPropertiesByAssetId, assetId);
            //noinspection unchecked
            assets.add(Asset.Builder.newInstance().properties(asMap(properties)).build());
        }

        return assets;
    }

    private void throwOnNotSupported(List<Criterion> criteria) {

        if (isSelectAll(criteria)) {
            return;
        }

        for (Criterion criterion : criteria) {
            if (!(criterion.getOperandLeft() instanceof String)) {
                throw new EdcException("PostgreSQL-Repository: Criterion left operand must be of type string");
            }
            if (!criterion.getOperator().equals("=") && !criterion.getOperator().equals("eq")) {
                throw new EdcException("PostgreSQL-Repository: Criterion operator must be Equals-Operator ('eq' or '=')");
            }
        }
    }

    private boolean isSelectAll(List<Criterion> criteria) {
        return criteria.size() == 1 &&
                criteria.get(0).equals(new Criterion("*", "=", "*"));
    }

    private Map<String, Object> asMap(List<Property> properties) {
        Map<String, Object> map = new HashMap<>();
        properties.forEach(p -> map.put(p.getKey(), p.getValue()));
        return map;
    }
}
