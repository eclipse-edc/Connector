/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.defaults.storage.assetindex;


import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.testfixtures.asset.AssetIndexTestBase;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

class InMemoryAssetIndexTest extends AssetIndexTestBase {
    private InMemoryAssetIndex index;

    @BeforeEach
    void setUp() {
        index = new InMemoryAssetIndex();
    }

    @Test
    void findById() {
        String id = UUID.randomUUID().toString();
        var testAsset = createAsset("barbaz", id);
        index.create(testAsset, createDataAddress(testAsset));

        var result = index.findById(id);

        assertThat(result).isNotNull().isEqualTo(testAsset);
    }

    @Test
    void findById_notfound() {
        String id = UUID.randomUUID().toString();
        var testAsset = createAsset("foobar", id);
        index.create(testAsset, createDataAddress(testAsset));

        var result = index.findById("not-exist");

        assertThat(result).isNull();
    }

    @Test
    void findAll_noQuerySpec() {
        var assets = IntStream.range(0, 10).mapToObj(i -> createAsset("test-asset", "id" + i))
                .peek(a -> index.create(a, createDataAddress(a))).collect(Collectors.toList());

        assertThat(index.queryAssets(QuerySpec.Builder.newInstance().build())).containsAll(assets);
    }

    @Test
    void findAll_withPaging_noSortOrderDesc() {
        IntStream.range(0, 10)
                .mapToObj(i -> createAsset("test-asset", "id" + i))
                .forEach(a -> index.create(a, createDataAddress(a)));

        var spec = QuerySpec.Builder.newInstance().sortOrder(SortOrder.DESC).offset(5).limit(2).build();

        var all = index.queryAssets(spec);
        assertThat(all).hasSize(2);
    }

    @Test
    void findAll_withPaging_noSortOrderAsc() {
        IntStream.range(0, 10)
                .mapToObj(i -> createAsset("test-asset", "id" + i))
                .forEach(a -> index.create(a, createDataAddress(a)));

        var spec = QuerySpec.Builder.newInstance().sortOrder(SortOrder.ASC).offset(3).limit(3).build();

        var all = index.queryAssets(spec);
        assertThat(all).hasSize(3);
    }

    @Test
    void findAll_withFiltering() {
        var assets = IntStream.range(0, 10)
                .mapToObj(i -> createAsset("test-asset", "id" + i))
                .peek(a -> index.create(a, createDataAddress(a)))
                .collect(Collectors.toList());

        var spec = QuerySpec.Builder.newInstance().equalsAsContains(false).filter(Asset.PROPERTY_ID + " = id1").build();
        assertThat(index.queryAssets(spec)).hasSize(1).containsExactly(assets.get(1));
    }

    @Test
    void findAll_withFiltering_limitExceedsResultSize() {
        IntStream.range(0, 10)
                .mapToObj(i -> createAsset("test-asset" + i))
                .forEach(a -> index.create(a, createDataAddress(a)));

        var spec = QuerySpec.Builder.newInstance()
                .sortOrder(SortOrder.ASC)
                .offset(15)
                .limit(10)
                .build();
        assertThat(index.queryAssets(spec)).isEmpty();
    }

    @Test
    void findAll_withSorting() {
        var assets = IntStream.range(0, 10)
                .mapToObj(i -> createAsset("test-asset", "id" + i))
                .peek(a -> index.create(a, createDataAddress(a)))
                .collect(Collectors.toList());

        var spec = QuerySpec.Builder.newInstance().sortField(Asset.PROPERTY_ID).sortOrder(SortOrder.ASC).build();
        assertThat(index.queryAssets(spec)).containsAll(assets);
    }

    @Test
    void findAll_withPrivateSorting() {
        var assets = IntStream.range(0, 10)
                .mapToObj(i -> createAssetBuilder("" + i).privateProperty("pKey", "pValue").build())
                .peek(a -> index.create(a, createDataAddress(a)))
                .collect(Collectors.toList());

        var spec = QuerySpec.Builder.newInstance().sortField("pKey").sortOrder(SortOrder.ASC).build();
        assertThat(index.queryAssets(spec)).containsAll(assets);
    }

    @Test
    void deleteById_whenMissing_returnsNull() {
        assertThat(index.deleteById("not-exists")).isNotNull().extracting(StoreResult::reason).isEqualTo(NOT_FOUND);
    }

    @Test
    void updateAsset_whenNotExists_returnsFailure() {
        var id = UUID.randomUUID().toString();
        var asset = createAsset("test-asset", id);
        var result = index.updateAsset(asset);
        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void updateAsset_whenExists_returnsUpdatedAsset() {
        var id = UUID.randomUUID().toString();
        var asset = createAsset("test-asset", id);
        var dataAddress = createDataAddress(asset);

        index.create(asset, dataAddress);

        var newAsset = createAsset("new-name", id);
        var result = index.updateAsset(newAsset);
        assertThat(result).isNotNull().extracting(StoreResult::getContent).usingRecursiveComparison().isEqualTo(newAsset);
    }

    @Test
    void updateDataAddress_whenNotExists_returnsFailure() {
        var id = UUID.randomUUID().toString();
        var address = createDataAddress(createAsset("test-asset", id));
        var result = index.updateDataAddress(id, address);
        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void updateDataAddress_whenExists_returnsUpdatedDataAddress() {
        var id = UUID.randomUUID().toString();
        var asset = createAsset("test-asset", id);
        var dataAddress = createDataAddress(asset);

        index.create(asset, dataAddress);

        dataAddress.getProperties().put("new", "value");
        var result = index.updateDataAddress(id, dataAddress);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isNotNull().usingRecursiveComparison().isEqualTo(dataAddress);
        assertThat(result.getContent().getProperties().get("new")).isEqualTo("value");
    }

    @Override
    protected Collection<String> getSupportedOperators() {
        return List.of("=", "in");
    }

    @Override
    protected AssetIndex getAssetIndex() {
        return index;
    }


}
