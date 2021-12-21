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

import org.eclipse.dataspaceconnector.clients.postgresql.asset.Repository;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@IntegrationTest
public class AssetQueryOperationTest extends AbstractOperationTest {
    private Repository repository;

    @BeforeEach
    public void setup() {
        repository = getRepository();
    }

    @Test
    public void testSelectAllAssetsQuery() throws SQLException {

        Asset asset1 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        Asset asset2 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        DataAddress dataAddress = DataAddress.Builder.newInstance().type("foo").build();

        repository.create(asset1, dataAddress);
        repository.create(asset2, dataAddress);

        Criterion selectAll = new Criterion("*", "=", "*");
        List<Asset> assets = repository.queryAssets(Collections.singletonList(selectAll));

        org.assertj.core.api.Assertions.assertThat(assets.stream().map(Asset::getId).collect(Collectors.toUnmodifiableList()))
                .contains(asset1.getId())
                .contains(asset2.getId());
    }

    @Test
    public void testSelectAssetsById() throws SQLException {

        Asset asset1 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        Asset asset2 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        DataAddress dataAddress = DataAddress.Builder.newInstance().type("foo").build();

        repository.create(asset1, dataAddress);
        repository.create(asset2, dataAddress);

        Criterion select = new Criterion(Asset.PROPERTY_ID, "=", asset1.getId());
        List<Asset> assets = repository.queryAssets(Collections.singletonList(select));

        org.assertj.core.api.Assertions.assertThat(assets.stream().map(Asset::getId).collect(Collectors.toUnmodifiableList()))
                .contains(asset1.getId());
    }

    @Test
    public void testSelectMultipleAssets() throws SQLException {

        Asset asset1 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType("pdf")
                .version("1.0.0")
                .build();

        Asset asset2 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType("pdf")
                .version("1.0.0")
                .build();

        DataAddress dataAddress = DataAddress.Builder.newInstance().type("foo").build();

        repository.create(asset1, dataAddress);
        repository.create(asset2, dataAddress);

        Criterion select1 = new Criterion(Asset.PROPERTY_CONTENT_TYPE, "=", "pdf");
        Criterion select2 = new Criterion(Asset.PROPERTY_VERSION, "=", "1.0.0");
        List<Asset> assets = repository.queryAssets(Arrays.asList(select1, select2));

        org.assertj.core.api.Assertions.assertThat(assets.stream().map(Asset::getId).collect(Collectors.toUnmodifiableList()))
                .contains(asset1.getId())
                .contains(asset2.getId());
    }

}
