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

package org.eclipse.edc.spi.testfixtures.asset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.asset.AssetEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;

/**
 * This is the minimum test specification that all {@link AssetIndex} implementations must support. All
 * {@link AssetIndex} tests, that actually utilize the target system (SQL, MongoDb,....) MUST inherit this class. Pure
 * unit tests need not inherit this, as they will likely heavily rely on mocks that require specific preparation.
 */
public abstract class AssetIndexTestBase {

    public AssetIndexTestBase() {
        var supportedOperators = getSupportedOperators();
        boolean hasLikeOperator = true;
        boolean hasInOperator = true;
        if (!supportedOperators.isEmpty()) {
            hasLikeOperator = supportedOperators.contains("like");
            hasInOperator = supportedOperators.contains("in");
        }
        System.setProperty("assetindex.supports.operator.like", String.valueOf(hasLikeOperator));
        System.setProperty("assetindex.supports.operator.in", String.valueOf(hasInOperator));
    }

    @Test
    @DisplayName("Accept an asset and a data address that don't exist yet")
    void acceptAssetAndDataAddress_doesNotExist() {
        var assetExpected = getAsset("id1");
        getAssetIndex().accept(assetExpected, getDataAddress());

        var assetFound = getAssetIndex().findById("id1");

        assertThat(assetFound).isNotNull();
        assertThat(assetFound).usingRecursiveComparison().isEqualTo(assetExpected);
    }

    @Test
    @DisplayName("Verify that the object was stored with the correct timestamp")
    void store_verifyTimestamp() {
        var asset = getAsset("test-asset");
        getAssetIndex().accept(asset, getDataAddress());

        var allAssets = getAssetIndex().queryAssets(QuerySpec.none());

        assertThat(allAssets).hasSize(1)
                .allSatisfy(a -> assertThat(a.getCreatedAt()).isNotEqualTo(0));
    }

    @Test
    @DisplayName("Accept an asset and a data address that already exist")
    void acceptAssetAndDataAddress_exists() {
        var asset = getAsset("id1");
        getAssetIndex().accept(asset, getDataAddress());
        getAssetIndex().accept(asset, getDataAddress());

        var assets = getAssetIndex().queryAssets(QuerySpec.none());

        assertThat(assets).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(asset);
    }

    @Test
    @DisplayName("Accept an asset entry that doesn't exist yet")
    void acceptAssetEntry_doesNotExist() {
        var assetExpected = getAsset("id1");
        getAssetIndex().accept(new AssetEntry(assetExpected, getDataAddress()));


        var assetFound = getAssetIndex().findById("id1");

        assertThat(assetFound).isNotNull();
        assertThat(assetFound).usingRecursiveComparison().isEqualTo(assetExpected);

    }

    @Test
    @DisplayName("Accept an asset entry that already exists")
    void acceptEntry_exists() {
        var asset = getAsset("id1");
        getAssetIndex().accept(new AssetEntry(asset, getDataAddress()));
        getAssetIndex().accept(asset, getDataAddress());

        var assets = getAssetIndex().queryAssets(QuerySpec.none());

        assertThat(assets).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(asset);
    }

    @Test
    @DisplayName("Delete an asset that doesn't exist")
    void deleteAsset_doesNotExist() {
        var assetDeleted = getAssetIndex().deleteById("id1");

        assertThat(assetDeleted).isNull();
    }

    @Test
    @DisplayName("Delete an asset that exists")
    void deleteAsset_exists() {
        var asset = getAsset("id1");
        getAssetIndex().accept(asset, getDataAddress());

        var assetDeleted = getAssetIndex().deleteById("id1");

        assertThat(assetDeleted).isNotNull();
        assertThat(assetDeleted).usingRecursiveComparison().isEqualTo(asset);

        assertThat(getAssetIndex().queryAssets(QuerySpec.none())).isEmpty();
    }

    @Test
    void count_withResults() {
        var assets = range(0, 5).mapToObj(i -> getAsset("id" + i));
        assets.forEach(a -> getAssetIndex().accept(a, getDataAddress()));

        var count = getAssetIndex().countAssets(QuerySpec.none());
        assertThat(count).isEqualTo(5);
    }

    @Test
    void count_withNoResults() {
        var count = getAssetIndex().countAssets(QuerySpec.none());
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Query assets with selector expression using the IN operator")
    void queryAsset_selectorExpression_in() {
        var asset1 = getAsset("id1");
        getAssetIndex().accept(asset1, getDataAddress());
        var asset2 = getAsset("id2");
        getAssetIndex().accept(asset2, getDataAddress());

        var assetsFound = getAssetIndex().queryAssets(AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "in", List.of("id1", "id2"))
                .build());

        assertThat(assetsFound).isNotNull().hasSize(2);
    }

    @Test
    @DisplayName("Query assets with selector expression using the IN operator, invalid righ-operand")
    void queryAsset_selectorExpression_invalidOperand() {
        var asset1 = getAsset("id1");
        getAssetIndex().accept(asset1, getDataAddress());
        var asset2 = getAsset("id2");
        getAssetIndex().accept(asset2, getDataAddress());

        var exception = catchException(() -> getAssetIndex().queryAssets(AssetSelectorExpression.Builder.newInstance().constraint(Asset.PROPERTY_ID, "in", "(id1, id2)").build())
                .collect(Collectors.toList())); // must collect, otherwise the stream may not get materialized

        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Query assets with selector expression using the LIKE operator")
    @EnabledIfSystemProperty(named = "assetindex.supports.operator.like", matches = "true", disabledReason = "This test only runs if the LIKE operator is supported")
    void queryAsset_selectorExpression_like() {
        var asset1 = getAsset("id1");
        getAssetIndex().accept(asset1, getDataAddress());
        var asset2 = getAsset("id2");
        getAssetIndex().accept(asset2, getDataAddress());

        var assetsFound = getAssetIndex().queryAssets(AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "LIKE", "id%")
                .build());

        assertThat(assetsFound).isNotNull().hasSize(2);
    }

    @Test
    @DisplayName("Query assets with selector expression using the LIKE operator on a json value")
    @EnabledIfSystemProperty(named = "assetindex.supports.operator.like", matches = "true", disabledReason = "This test only runs if the LIKE operator is supported")
    void queryAsset_selectorExpression_likeJson() throws JsonProcessingException {
        var asset = getAsset("id1");
        asset.getProperties().put("myjson", new ObjectMapper().writeValueAsString(new TestObject("test123", 42, false)));
        getAssetIndex().accept(asset, getDataAddress());

        var assetsFound = getAssetIndex().queryAssets(AssetSelectorExpression.Builder.newInstance()
                .constraint("myjson", "LIKE", "%test123%")
                .build());

        assertThat(assetsFound).usingRecursiveFieldByFieldElementComparator().containsExactly(asset);
    }

    @Test
    @DisplayName("Query assets with query spec")
    void queryAsset_querySpec() {
        for (var i = 1; i <= 10; i++) {
            var asset = getAsset("id" + i);
            getAssetIndex().accept(asset, getDataAddress());
        }

        var assetsFound = getAssetIndex().queryAssets(getQuerySpec());

        assertThat(assetsFound).isNotNull().hasSize(3);
    }

    @Test
    @DisplayName("Query assets with query spec where the property (=leftOperand) does not exist")
    void queryAsset_querySpec_nonExistProperty() {
        var asset = getAsset("id1");
        getAssetIndex().accept(asset, getDataAddress());

        var qs = QuerySpec.Builder
                .newInstance()
                .filter(List.of(new Criterion("noexist", "=", "42")))
                .build();
        assertThat(getAssetIndex().queryAssets(qs)).isEmpty();
    }

    @Test
    @DisplayName("Query assets with selector expression using the LIKE operator on a json value")
    @EnabledIfSystemProperty(named = "assetindex.supports.operator.like", matches = "true", disabledReason = "This test only runs if the LIKE operator is supported")
    void queryAsset_querySpec_likeJson() throws JsonProcessingException {
        var asset = getAsset("id1");
        asset.getProperties().put("myjson", new ObjectMapper().writeValueAsString(new TestObject("test123", 42, false)));
        getAssetIndex().accept(asset, getDataAddress());

        var assetsFound = getAssetIndex().queryAssets(QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("myjson", "LIKE", "%test123%")))
                .build());

        assertThat(assetsFound).usingRecursiveFieldByFieldElementComparator().containsExactly(asset);
    }

    @Test
    @DisplayName("Query assets with query spec where the value (=rightOperand) does not exist")
    void queryAsset_querySpec_nonExistValue() {
        var asset = getAsset("id1");
        asset.getProperties().put("someprop", "someval");
        getAssetIndex().accept(asset, getDataAddress());

        var qs = QuerySpec.Builder
                .newInstance()
                .filter(List.of(new Criterion("someprop", "=", "some-other-val")))
                .build();
        assertThat(getAssetIndex().queryAssets(qs)).isEmpty();
    }

    @Test
    @DisplayName("Query assets with query spec and short asset count")
    void queryAsset_querySpecShortCount() {
        range(1, 5).forEach((item) -> {
            var asset = getAsset("id" + item);
            getAssetIndex().accept(asset, getDataAddress());
        });

        var assetsFound = getAssetIndex().queryAssets(getQuerySpec());

        assertThat(assetsFound).isNotNull().hasSize(2);
    }

    @Test
    void queryAsset_withFilterExpression() {
        var qs = QuerySpec.Builder.newInstance().filter(List.of(
                new Criterion("version", "=", "2.0"),
                new Criterion("contenttype", "=", "whatever")
        ));

        var asset = getAsset("id1");
        asset.getProperties().put("version", "2.0");
        asset.getProperties().put("contenttype", "whatever");
        getAssetIndex().accept(asset, getDataAddress());

        var result = getAssetIndex().queryAssets(qs.build());
        assertThat(result).usingRecursiveFieldByFieldElementComparator().containsOnly(asset);

    }

    @Test
    @DisplayName("Find an asset that doesn't exist")
    void findAsset_doesNotExist() {
        assertThat(getAssetIndex().findById("id1")).isNull();
    }

    @Test
    @DisplayName("Find an asset that exists")
    void findAsset_exists() {
        var asset = getAsset("id1");
        getAssetIndex().accept(asset, getDataAddress());

        var assetFound = getAssetIndex().findById("id1");

        assertThat(assetFound).isNotNull();
        assertThat(assetFound).usingRecursiveComparison().isEqualTo(asset);
    }

    @Test
    @DisplayName("Find a data address that doesn't exist")
    void resolveDataAddress_doesNotExist() {
        assertThat(getAssetIndex().resolveForAsset("id1")).isNull();
    }

    @Test
    @DisplayName("Find a data address that exists")
    void resolveDataAddress_exists() {
        var asset = getAsset("id1");
        var dataAddress = getDataAddress();
        getAssetIndex().accept(asset, dataAddress);

        var dataAddressFound = getAssetIndex().resolveForAsset("id1");

        assertThat(dataAddressFound).isNotNull();
        assertThat(dataAddressFound).usingRecursiveComparison().isEqualTo(dataAddress);
    }


    /**
     * Returns an array of all operators supported by a particular AssetIndex. If no limitations or constraints exist
     * (i.e. the AssetIndex supports all operators), then an empty list can be returned. Note that the operators MUST be
     * lowercase!
     */
    protected Collection<String> getSupportedOperators() {
        return Collections.emptyList();
    }

    /**
     * Returns the SuT i.e. the fully constructed instance of the {@link AssetIndex}
     */
    protected abstract AssetIndex getAssetIndex();

    private Asset getAsset(String id) {
        return Asset.Builder.newInstance()
                .id(id)
                .createdAt(Clock.systemUTC().millis())
                .property("key" + id, "value" + id)
                .contentType("type")
                .build();
    }

    private DataAddress getDataAddress() {
        return DataAddress.Builder.newInstance()
                .type("type")
                .property("key", "value")
                .build();
    }

    private QuerySpec getQuerySpec() {
        return QuerySpec.Builder.newInstance()
                .limit(3)
                .offset(2)
                .build();
    }
}

