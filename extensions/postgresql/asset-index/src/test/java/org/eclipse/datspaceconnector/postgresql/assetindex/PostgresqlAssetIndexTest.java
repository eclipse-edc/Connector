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

package org.eclipse.datspaceconnector.postgresql.assetindex;

import org.assertj.core.api.Assertions;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.Repository;
import org.eclipse.dataspaceconnector.postgresql.assetindex.PostgresqlAssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PostgresqlAssetIndexTest {
    private AssetIndex assetIndex;

    // mocks
    private Repository repository;

    @BeforeEach
    public void setup() {
        repository = Mockito.mock(Repository.class);
        assetIndex = new PostgresqlAssetIndex(repository);
    }

    @Test
    public void testQueryExpression() throws SQLException {
        List<Criterion> criteria = new ArrayList<>();
        criteria.add(new Criterion("foo", "=", "bar"));

        Asset asset = Asset.Builder.newInstance().build();
        List<Asset> assets = new ArrayList<>();
        assets.add(asset);

        Mockito.when(repository.queryAssets(criteria)).thenReturn(assets);

        AssetSelectorExpression expression =
                AssetSelectorExpression.Builder.newInstance().criteria(criteria).build();
        Stream<Asset> result = assetIndex.queryAssets(expression);

        Assertions.assertThat(result.map(Asset::getId).collect(Collectors.toUnmodifiableList()))
                .contains(asset.getId());
    }

    @Test
    public void testQueryCriteria() throws SQLException {
        List<Criterion> criteria = new ArrayList<>();
        criteria.add(new Criterion("foo", "=", "bar"));

        Asset asset = Asset.Builder.newInstance().build();
        List<Asset> assets = new ArrayList<>();
        assets.add(asset);

        Mockito.when(repository.queryAssets(criteria)).thenReturn(assets);

        Stream<Asset> result = assetIndex.queryAssets(criteria);

        Assertions.assertThat(result.map(Asset::getId).collect(Collectors.toUnmodifiableList()))
                .contains(asset.getId());
    }

    @Test
    public void testQueryId() throws SQLException {
        Asset asset = Asset.Builder.newInstance().build();
        List<Asset> assets = new ArrayList<>();
        assets.add(asset);

        Mockito.when(repository.queryAssets(
                Collections.singletonList(new Criterion(Asset.PROPERTY_ID, "=", asset.getId()))
        )).thenReturn(assets);

        Asset result = assetIndex.findById(asset.getId());

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(asset.getId());
    }
}
