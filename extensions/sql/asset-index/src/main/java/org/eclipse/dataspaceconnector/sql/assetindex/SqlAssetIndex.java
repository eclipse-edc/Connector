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

package org.eclipse.dataspaceconnector.sql.assetindex;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.sql.operations.asset.SqlAssetQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.sql.DataSource;

public class SqlAssetIndex implements AssetIndex {

    private final DataSource dataSource;

    public SqlAssetIndex(@NotNull DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        return queryAssets(expression.getCriteria());
    }

    @Override
    public Stream<Asset> queryAssets(List<Criterion> criteria) {
        try {
            Map<String, Object> filters = new HashMap<>();

            // TODO see whether criteria must be checked for empty and select_all
            // https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/pull/501

            for (Criterion criterion : criteria) {
                if (!criterion.getOperator().equals("=") && !criterion.getOperator().equals("eq")) {
                    throw new EdcException(String.format("Unsupported operator '%s'", criterion.getOperator()));
                }

                if (!(criterion.getOperandLeft() instanceof String)) {
                    throw new EdcException(String.format("Unsupported left operand. Must be String, was %s", criterion.getOperandLeft().getClass().getName()));
                }

                filters.put((String) criterion.getOperandLeft(), criterion.getOperandRight());
            }

            Connection connection = dataSource.getConnection();

            return new SqlAssetQuery(connection)
                    .execute(filters)
                    .stream();
        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public @Nullable Asset findById(String assetId) {
        try {
            Connection connection = dataSource.getConnection();
            Map<String, Object> filters = Collections.singletonMap(Asset.PROPERTY_ID, assetId);
            return new SqlAssetQuery(connection)
                    .execute(filters)
                    .stream()
                    .findFirst()
                    .orElse(null);

        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }
}
