/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression.SELECT_ALL;

class InMemoryAssetIndexTest {
    private InMemoryAssetIndex index;

    @BeforeEach
    void setUp() {
        index = new InMemoryAssetIndex();
    }

    @Test
    void queryAssets() {
        var testAsset = createAsset("foobar");
        index.accept(testAsset, createDataAddress(testAsset));
        var assets = index.queryAssets(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_NAME, "foobar").build());

        assertThat(assets).hasSize(1).containsExactly(testAsset);
    }

    @Test
    void queryAssets_notFound() {
        var testAsset = createAsset("foobar");
        index.accept(testAsset, createDataAddress(testAsset));
        var assets = index.queryAssets(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_NAME, "barbaz").build());

        assertThat(assets).isEmpty();
    }

    @Test
    void queryAssets_fieldNull() {
        var testAsset = createAsset("foobar");
        index.accept(testAsset, createDataAddress(testAsset));

        var assets = index.queryAssets(AssetSelectorExpression.Builder.newInstance().whenEquals("description", "barbaz").build());

        assertThat(assets).isEmpty();
    }

    @Test
    void queryAssets_multipleFound() {
        var testAsset1 = createAsset("foobar");
        var testAsset2 = createAsset("barbaz");
        var testAsset3 = createAsset("barbaz");
        index.accept(testAsset1, createDataAddress(testAsset1));
        index.accept(testAsset2, createDataAddress(testAsset2));
        index.accept(testAsset3, createDataAddress(testAsset3));

        var assets = index.queryAssets(AssetSelectorExpression.Builder.newInstance()
                .whenEquals(Asset.PROPERTY_NAME, "barbaz")
                .whenEquals(Asset.PROPERTY_VERSION, "1")
                .build());

        assertThat(assets).hasSize(2).containsExactlyInAnyOrder(testAsset2, testAsset3);
    }

    @Test
    void queryAssets_selectAll_shouldReturnAll() {
        var testAsset1 = createAsset("barbaz");
        index.accept(testAsset1, createDataAddress(testAsset1));

        var testAsset2 = createAsset("foobar");
        index.accept(testAsset2, createDataAddress(testAsset2));

        var results = index.queryAssets(SELECT_ALL);

        assertThat(results).containsExactlyInAnyOrder(testAsset1, testAsset2);
    }

    @Test
    void findById() {
        String id = UUID.randomUUID().toString();
        var testAsset = createAsset("barbaz", id);
        index.accept(testAsset, createDataAddress(testAsset));

        var result = index.findById(id);

        assertThat(result).isNotNull().isEqualTo(testAsset);
    }


    @Test
    void findById_notfound() {
        String id = UUID.randomUUID().toString();
        var testAsset = createAsset("foobar", id);
        index.accept(testAsset, createDataAddress(testAsset));

        var result = index.findById("not-exist");

        assertThat(result).isNull();
    }

    @Test
    void queryAsset_operatorIn() {
        var testAsset1 = createAsset("foobar");
        var testAsset2 = createAsset("barbaz");
        var testAsset3 = createAsset("barbaz");
        index.accept(testAsset1, createDataAddress(testAsset1));
        index.accept(testAsset2, createDataAddress(testAsset2));
        index.accept(testAsset3, createDataAddress(testAsset3));

        var inExpr = format("(  %s )", String.join(", ", List.of(testAsset1.getId(), testAsset2.getId())));
        var selector = AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "IN", inExpr)
                .build();

        var assets = index.queryAssets(selector);

        assertThat(assets).hasSize(2).containsExactlyInAnyOrder(testAsset1, testAsset2);
    }

    @Test
    void queryAsset_operatorIn_notIn() {
        var testAsset1 = createAsset("foobar");
        var testAsset2 = createAsset("barbaz");
        var testAsset3 = createAsset("barbaz");
        index.accept(testAsset1, createDataAddress(testAsset1));
        index.accept(testAsset2, createDataAddress(testAsset2));
        index.accept(testAsset3, createDataAddress(testAsset3));

        var inExpr = format("(  %s )", String.join(", ", List.of("test-id1", "test-id2")));
        var selector = AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "IN", inExpr)
                .build();

        var assets = index.queryAssets(selector);

        assertThat(assets).isEmpty();
    }

    @Test
    void queryAsset_operatorIn_noBrackets() {
        var testAsset1 = createAsset("foobar");
        var testAsset2 = createAsset("barbaz");
        var testAsset3 = createAsset("barbaz");
        index.accept(testAsset1, createDataAddress(testAsset1));
        index.accept(testAsset2, createDataAddress(testAsset2));
        index.accept(testAsset3, createDataAddress(testAsset3));

        var inExpr = String.join(", ", List.of(testAsset1.getId(), testAsset2.getId()));
        var selector = AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "IN", inExpr)
                .build();

        var assets = index.queryAssets(selector);

        assertThat(assets).hasSize(2).containsExactlyInAnyOrder(testAsset1, testAsset2);
    }

    @Test
    void queryAsset_operatorIn_noBracketsNoSpaces() {
        var testAsset1 = createAsset("foobar");
        var testAsset2 = createAsset("barbaz");
        var testAsset3 = createAsset("barbaz");
        index.accept(testAsset1, createDataAddress(testAsset1));
        index.accept(testAsset2, createDataAddress(testAsset2));
        index.accept(testAsset3, createDataAddress(testAsset3));

        var inExpr = String.join(",", List.of(testAsset1.getId(), testAsset2.getId()));
        var selector = AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "IN", inExpr)
                .build();

        var assets = index.queryAssets(selector);

        assertThat(assets).hasSize(2).containsExactlyInAnyOrder(testAsset1, testAsset2);
    }

    @Test
    void findAll_noQuerySpec() {
        var assets = IntStream.range(0, 10).mapToObj(i -> createAsset("test-asset", "id" + i))
                .peek(a -> index.accept(a, createDataAddress(a))).collect(Collectors.toList());


        assertThat(index.queryAssets(QuerySpec.Builder.newInstance().build())).containsAll(assets);
    }

    @Test
    void findAll_withPaging_noSortOrderDesc() {
        IntStream.range(0, 10)
                .mapToObj(i -> createAsset("test-asset", "id" + i))
                .forEach(a -> index.accept(a, createDataAddress(a)));

        var spec = QuerySpec.Builder.newInstance().sortOrder(SortOrder.DESC).offset(5).limit(2).build();

        var all = index.queryAssets(spec);
        assertThat(all).hasSize(2);
    }

    @Test
    void findAll_withPaging_noSortOrderAsc() {
        IntStream.range(0, 10)
                .mapToObj(i -> createAsset("test-asset", "id" + i))
                .forEach(a -> index.accept(a, createDataAddress(a)));

        var spec = QuerySpec.Builder.newInstance().sortOrder(SortOrder.ASC).offset(3).limit(3).build();

        var all = index.queryAssets(spec);
        assertThat(all).hasSize(3);
    }

    @Test
    void findAll_withFiltering() {
        var assets = IntStream.range(0, 10)
                .mapToObj(i -> createAsset("test-asset", "id" + i))
                .peek(a -> index.accept(a, createDataAddress(a)))
                .collect(Collectors.toList());

        var spec = QuerySpec.Builder.newInstance().equalsAsContains(false).filter(Asset.PROPERTY_ID + " = id1").build();
        assertThat(index.queryAssets(spec)).hasSize(1).containsExactly(assets.get(1));
    }


    @Test
    void findAll_withFiltering_limitExceedsResultSize() {
        IntStream.range(0, 10)
                .mapToObj(i -> createAsset("test-asset" + i))
                .forEach(a -> index.accept(a, createDataAddress(a)));

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
                .peek(a -> index.accept(a, createDataAddress(a)))
                .collect(Collectors.toList());

        var spec = QuerySpec.Builder.newInstance().sortField(Asset.PROPERTY_ID).sortOrder(SortOrder.ASC).build();
        assertThat(index.queryAssets(spec)).containsAll(assets);
    }

    @Test
    void deleteById_whenPresent_deletes() {
        var asset = createAsset("foobar");
        index.accept(asset, createDataAddress(asset));
        var deletedAsset = index.deleteById(asset.getId());

        assertThat(deletedAsset).isEqualTo(asset);
        var assetSelector = AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_NAME, asset.getName()).build();
        var assets = index.queryAssets(assetSelector);
        assertThat(assets).isEmpty();
    }

    @Test
    void deleteById_whenMissing_returnsNull() {
        assertThat(index.deleteById("not-exists")).isNull();
    }

    @NotNull
    private Asset createAsset(String name) {
        return createAsset(name, UUID.randomUUID().toString());
    }

    @NotNull
    private Asset createAsset(String name, String id) {
        return createAsset(name, id, "contentType");
    }

    @NotNull
    private Asset createAsset(String name, String id, String contentType) {
        return Asset.Builder.newInstance().id(id).name(name).version("1").contentType(contentType).build();
    }

    @NotNull
    private DataAddress createDataAddress(Asset asset) {
        return DataAddress.Builder.newInstance()
                .keyName("test-keyname")
                .type(asset.getContentType())
                .build();
    }
}
