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

package org.eclipse.dataspaceconnector.postgresql.assetindex;

import org.eclipse.dataspaceconnector.clients.postgresql.asset.Repository;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class PostgresqlAssetIndex implements AssetIndex {

    private final Repository repository;

    public PostgresqlAssetIndex(@NotNull Repository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        try {
            return repository
                    .queryAssets(expression.getCriteria())
                    .stream();
        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public Stream<Asset> queryAssets(List<Criterion> criteria) {
        try {
            return repository
                    .queryAssets(criteria)
                    .stream();
        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public @Nullable Asset findById(String assetId) {
        try {
            return repository
                    .queryAssets(Collections.singletonList(
                            new Criterion(Asset.PROPERTY_ID, "=", assetId)
                    ))
                    .stream()
                    .findFirst()
                    .orElse(null);
        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }
}
