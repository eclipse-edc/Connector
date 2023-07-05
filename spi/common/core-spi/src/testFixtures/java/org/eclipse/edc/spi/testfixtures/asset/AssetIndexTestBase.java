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
import org.assertj.core.api.Assertions;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Clock;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.DUPLICATE_KEYS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

/**
 * This is the minimum test specification that all {@link AssetIndex} implementations must support. All
 * {@link AssetIndex} tests, that actually utilize the target system (SQL, MongoDb,....) MUST inherit this class. Pure
 * unit tests need not inherit this, as they will likely heavily rely on mocks that require specific preparation.
 */
public abstract class AssetIndexTestBase {

    public AssetIndexTestBase() {
        var supportedOperators = getSupportedOperators();
        var hasLikeOperator = true;
        var hasInOperator = true;
        if (!supportedOperators.isEmpty()) {
            hasLikeOperator = supportedOperators.contains("like");
            hasInOperator = supportedOperators.contains("in");
        }
        System.setProperty("assetindex.supports.operator.like", String.valueOf(hasLikeOperator));
        System.setProperty("assetindex.supports.operator.in", String.valueOf(hasInOperator));
    }

    @Test
    void create_shouldStoreAsset() {
        var assetExpected = getAsset("id1");
        getAssetIndex().create(assetExpected);

        var assetFound = getAssetIndex().findById("id1");

        assertThat(assetFound).isNotNull();
        assertThat(assetFound).usingRecursiveComparison().isEqualTo(assetExpected);
    }

    @Test
    @DisplayName("Verify that storing an asset fails if it already exists")
    void create_exists() {
        var asset = createAsset("test-asset", UUID.randomUUID().toString());
        var assetIndex = getAssetIndex();
        assetIndex.create(asset);

        var result = assetIndex.create(asset);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.reason()).isEqualTo(ALREADY_EXISTS);
        //assert that this replaces the previous data address
        assertThat(getAssetIndex().queryAssets(QuerySpec.none())).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .contains(asset);
    }

    @Test
    @DisplayName("Verify that multiple assets can be stored")
    void create_shouldAddMultipleAssets() {
        var asset1 = createAssetBuilder("id1").name("asset1").dataAddress(createDataAddress()).build();
        var asset2 = createAssetBuilder("id2").name("asset2").dataAddress(createDataAddress()).build();

        var assetIndex = getAssetIndex();
        var results = Stream.of(asset1, asset2).map(assetIndex::create);

        assertThat(results).allSatisfy(sr -> assertThat(sr.succeeded()).isTrue());

        assertThat(assetIndex.queryAssets(QuerySpec.none())).hasSize(2)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(asset1, asset2);
    }

    @Test
    @DisplayName("Verify that the object was stored with the correct timestamp")
    void create_verifyTimestamp() {
        var asset = getAsset("test-asset");
        getAssetIndex().create(asset);

        var allAssets = getAssetIndex().queryAssets(QuerySpec.none());

        assertThat(allAssets).hasSize(1)
                .allSatisfy(a -> assertThat(a.getCreatedAt()).isNotEqualTo(0));
    }

    @Test
    @DisplayName("Verify that creating an asset that contains duplicate keys in properties and private properties fails")
    void createAsset_withDuplicatePropertyKeys() {
        var asset = createAssetBuilder("id1")
                .property("testproperty", "testvalue")
                .privateProperty("testproperty", "testvalue")
                .build();

        var result = getAssetIndex().create(asset, createDataAddress());
        assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(DUPLICATE_KEYS);
    }

    @Test
    @DisplayName("Delete an asset that doesn't exist")
    void deleteById_doesNotExist() {
        var assetDeleted = getAssetIndex().deleteById("id1");

        Assertions.assertThat(assetDeleted).isNotNull().extracting(StoreResult::reason).isEqualTo(NOT_FOUND);
    }

    @Test
    @DisplayName("Delete an asset that exists")
    void deleteById_exists() {
        var asset = getAsset("id1");
        getAssetIndex().create(asset);

        var assetDeleted = getAssetIndex().deleteById("id1");

        Assertions.assertThat(assetDeleted).isNotNull().extracting(StoreResult::succeeded).isEqualTo(true);
        assertThat(assetDeleted.getContent()).usingRecursiveComparison().isEqualTo(asset);

        assertThat(getAssetIndex().queryAssets(QuerySpec.none())).isEmpty();
    }

    @Test
    void count_withResults() {
        var assets = range(0, 5).mapToObj(i -> getAsset("id" + i));
        assets.forEach(a -> getAssetIndex().create(a));
        var criteria = Collections.<Criterion>emptyList();

        var count = getAssetIndex().countAssets(criteria);

        assertThat(count).isEqualTo(5);
    }

    @Test
    void count_withNoResults() {
        var criteria = Collections.<Criterion>emptyList();

        var count = getAssetIndex().countAssets(criteria);

        assertThat(count).isEqualTo(0);
    }

    @Test
    void queryAssets_shouldReturnAllTheAssets_whenQuerySpecIsEmpty() {
        var assets = IntStream.range(0, 5)
                .mapToObj(i -> createAsset("test-asset", "id" + i))
                .peek(a -> getAssetIndex().create(a)).toList();

        var result = getAssetIndex().queryAssets(QuerySpec.none());

        var result1 = result.toList();
        assertThat(result1).hasSize(5).usingRecursiveFieldByFieldElementComparator().containsAll(assets);
    }

    @Test
    @DisplayName("Query assets with query spec")
    void queryAssets_limit() {
        for (var i = 1; i <= 10; i++) {
            var asset = getAsset("id" + i);
            getAssetIndex().create(asset);
        }
        var querySpec = QuerySpec.Builder.newInstance().limit(3).offset(2).build();

        var assetsFound = getAssetIndex().queryAssets(querySpec);

        assertThat(assetsFound).isNotNull().hasSize(3);
    }

    @Test
    @DisplayName("Query assets with query spec and short asset count")
    void queryAssets_shortCount() {
        range(1, 5).mapToObj(it -> getAsset("id" + it)).forEach(asset -> getAssetIndex().create(asset));
        var querySpec = QuerySpec.Builder.newInstance()
                .limit(3)
                .offset(2)
                .build();

        var assetsFound = getAssetIndex().queryAssets(querySpec);

        assertThat(assetsFound).isNotNull().hasSize(2);
    }

    @Test
    void queryAssets_shouldReturnNoAssets_whenOffsetIsOutOfBounds() {
        range(1, 5).mapToObj(it -> getAsset("id" + it)).forEach(asset -> getAssetIndex().create(asset));
        var querySpec = QuerySpec.Builder.newInstance()
                .limit(3)
                .offset(5)
                .build();

        var assetsFound = getAssetIndex().queryAssets(querySpec);

        assertThat(assetsFound).isEmpty();
    }

    @Test
    void queryAssets_shouldThrowException_whenUnsupportedOperator() {
        var asset = getAsset("id1");
        getAssetIndex().create(asset);
        var unsupportedOperator = new Criterion(Asset.PROPERTY_ID, "unsupported", "42");

        assertThatThrownBy(() -> getAssetIndex().queryAssets(filter(unsupportedOperator)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void queryAssets_shouldReturnEmptyStream_whenLeftOperandDoesNotExist() {
        var asset = getAsset("id1");
        getAssetIndex().create(asset);
        var notExistingProperty = new Criterion("noexist", "=", "42");

        var assets = getAssetIndex().queryAssets(filter(notExistingProperty));

        assertThat(assets).isEmpty();
    }

    @Test
    @DisplayName("Query assets with query spec where the value (=rightOperand) does not exist")
    void queryAssets_nonExistValue() {
        var asset = getAsset("id1");
        asset.getProperties().put("someprop", "someval");
        getAssetIndex().create(asset);
        var notExistingValue = new Criterion("someprop", "=", "some-other-val");

        var assets = getAssetIndex().queryAssets(filter(notExistingValue));

        assertThat(assets).isEmpty();
    }

    @Test
    @DisplayName("Verifies an asset query, that contains a filter expression")
    void queryAssets_withFilterExpression() {
        var expected = createAssetBuilder("id1").property("version", "2.0").property("contenttype", "whatever").build();
        var differentVersion = createAssetBuilder("id2").property("version", "2.1").property("contenttype", "whatever").build();
        var differentContentType = createAssetBuilder("id3").property("version", "2.0").property("contenttype", "different").build();
        getAssetIndex().create(expected);
        getAssetIndex().create(differentVersion);
        getAssetIndex().create(differentContentType);
        var filter = filter(
                new Criterion("version", "=", "2.0"),
                new Criterion("contenttype", "=", "whatever")
        );

        var assets = getAssetIndex().queryAssets(filter);

        assertThat(assets).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsOnly(expected);
    }

    @Test
    @DisplayName("Verify an asset query based on an Asset property, where the property value is actually a complex object")
    @EnabledIfSystemProperty(named = "assetindex.supports.operator.like", matches = "true", disabledReason = "This test only runs if the LIKE operator is supported")
    void query_assetPropertyAsObject() {
        var dataAddress = createDataAddress();
        var asset = createAssetBuilder("id1").dataAddress(dataAddress).build();
        asset.getProperties().put("testobj", new TestObject("test123", 42, false));
        getAssetIndex().create(asset);

        var assetsFound = getAssetIndex().queryAssets(QuerySpec.Builder.newInstance()
                .filter(criterion("testobj", "like", "%test1%"))
                .build());

        assertThat(assetsFound).hasSize(1).first().usingRecursiveComparison().isEqualTo(asset);
        assertThat(asset.getProperty("testobj")).isInstanceOf(TestObject.class);
    }

    @Test
    void queryAssets_multipleFound() {
        var testAsset1 = createAsset("foobar");
        var testAsset2 = createAsset("barbaz");
        var testAsset3 = createAsset("barbaz");
        getAssetIndex().create(testAsset1);
        getAssetIndex().create(testAsset2);
        getAssetIndex().create(testAsset3);
        var criterion = new Criterion(Asset.PROPERTY_NAME, "=", "barbaz");

        var assets = getAssetIndex().queryAssets(filter(criterion));

        assertThat(assets).hasSize(2).map(Asset::getId).containsExactlyInAnyOrder(testAsset2.getId(), testAsset3.getId());
    }

    @Test
    @DisplayName("Query assets using the IN operator")
    void queryAssets_in() {
        getAssetIndex().create(getAsset("id1"));
        getAssetIndex().create(getAsset("id2"));
        var criterion = new Criterion(Asset.PROPERTY_ID, "in", List.of("id1", "id2"));

        var assetsFound = getAssetIndex().queryAssets(filter(criterion));

        assertThat(assetsFound).isNotNull().hasSize(2);
    }

    @Test
    @DisplayName("Query assets using the IN operator, invalid righ-operand")
    void queryAssets_in_shouldThrowException_whenInvalidRightOperand() {
        var asset1 = getAsset("id1");
        getAssetIndex().create(asset1);
        var asset2 = getAsset("id2");
        getAssetIndex().create(asset2);
        var invalidRightOperand = new Criterion(Asset.PROPERTY_ID, "in", "(id1, id2)");

        assertThatThrownBy(() -> getAssetIndex().queryAssets(filter(invalidRightOperand)).toList())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void queryAssets_withSorting() {
        var assets = IntStream.range(9, 12)
                .mapToObj(i -> createAsset("test-asset", "id" + i))
                .peek(a -> getAssetIndex().create(a))
                .toList();
        var spec = QuerySpec.Builder.newInstance()
                .sortField(Asset.PROPERTY_ID)
                .sortOrder(SortOrder.ASC)
                .build();

        var result = getAssetIndex().queryAssets(spec);

        assertThat(result).usingRecursiveFieldByFieldElementComparator().containsAll(assets);
    }

    @Test
    void queryAssets_withPrivateSorting() {
        var assets = IntStream.range(0, 10)
                .mapToObj(i -> createAssetBuilder(String.valueOf(i)).privateProperty("pKey", "pValue").build())
                .peek(a -> getAssetIndex().create(a))
                .collect(Collectors.toList());

        var spec = QuerySpec.Builder.newInstance().sortField("pKey").sortOrder(SortOrder.ASC).build();

        var result = getAssetIndex().queryAssets(spec);

        assertThat(result).usingRecursiveFieldByFieldElementComparator().containsAll(assets);
    }

    @Test
    @DisplayName("Query assets using the LIKE operator")
    @EnabledIfSystemProperty(named = "assetindex.supports.operator.like", matches = "true", disabledReason = "This test only runs if the LIKE operator is supported")
    void queryAssets_like() {
        var asset1 = getAsset("id1");
        getAssetIndex().create(asset1);
        var asset2 = getAsset("id2");
        getAssetIndex().create(asset2);
        var criterion = new Criterion(Asset.PROPERTY_ID, "LIKE", "id%");

        var assetsFound = getAssetIndex().queryAssets(filter(criterion));

        assertThat(assetsFound).isNotNull().hasSize(2);
    }

    @Test
    @DisplayName("Query assets using the LIKE operator on a json value")
    @EnabledIfSystemProperty(named = "assetindex.supports.operator.like", matches = "true", disabledReason = "This test only runs if the LIKE operator is supported")
    void queryAssets_likeJson() throws JsonProcessingException {
        var asset = getAsset("id1");
        asset.getProperties().put("myjson", new ObjectMapper().writeValueAsString(new TestObject("test123", 42, false)));
        getAssetIndex().create(asset);
        var criterion = new Criterion("myjson", "LIKE", "%test123%");

        var assetsFound = getAssetIndex().queryAssets(filter(criterion));

        assertThat(assetsFound).usingRecursiveFieldByFieldElementComparator().containsExactly(asset);
    }

    @Test
    @DisplayName("Query assets using two criteria, each with the LIKE operator on a nested json value")
    @EnabledIfSystemProperty(named = "assetindex.supports.operator.like", matches = "true", disabledReason = "This test only runs if the LIKE operator is supported")
    void queryAssets_likeJson_withComplexObject() throws JsonProcessingException {
        var asset = getAsset("id1");
        var jsonObject = Map.of("root", Map.of("key1", "value1", "nested1", Map.of("key2", "value2", "key3", Map.of("theKey", "theValue, this is what we're looking for"))));
        asset.getProperties().put("myProp", new ObjectMapper().writeValueAsString(jsonObject));
        getAssetIndex().create(asset);
        var criterion1 = new Criterion("myProp", "LIKE", "%is%what%");
        var criterion2 = new Criterion("myProp", "LIKE", "%we're%looking%");

        var assetsFound = getAssetIndex().queryAssets(filter(criterion1, criterion2));

        assertThat(assetsFound).usingRecursiveFieldByFieldElementComparator().containsExactly(asset);
    }

    @Test
    void findById_shouldReturnAsset() {
        var id = UUID.randomUUID().toString();
        var asset = getAsset(id);
        getAssetIndex().create(asset);

        var assetFound = getAssetIndex().findById(id);

        assertThat(assetFound).isNotNull();
        assertThat(assetFound).usingRecursiveComparison().isEqualTo(asset);
    }

    @Test
    void findById_shouldReturnNull_whenAssetDoesNotExist() {
        var result = getAssetIndex().findById("unexistent");

        assertThat(result).isNull();
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
        getAssetIndex().create(asset);

        var dataAddressFound = getAssetIndex().resolveForAsset("id1");

        assertThat(dataAddressFound).isNotNull();
        assertThat(dataAddressFound).usingRecursiveComparison().isEqualTo(dataAddress);
    }

    @Test
    @DisplayName("Update Asset that does not yet exist")
    void updateAsset_doesNotExist() {
        var id = "id1";
        var assetExpected = getAsset(id);
        var assetIndex = getAssetIndex();

        var updated = assetIndex.updateAsset(assetExpected);
        Assertions.assertThat(updated).isNotNull().extracting(StoreResult::succeeded).isEqualTo(false);
    }

    @Test
    @DisplayName("Update an Asset that exists, adding a property")
    void updateAsset_exists_addsProperty() {
        var id = "id1";
        var asset = getAsset(id);
        var assetIndex = getAssetIndex();
        assetIndex.create(asset);

        assertThat(assetIndex.countAssets(List.of())).isEqualTo(1);

        asset.getProperties().put("newKey", "newValue");
        var updated = assetIndex.updateAsset(asset);

        Assertions.assertThat(updated).isNotNull();

        var assetFound = getAssetIndex().findById("id1");

        assertThat(assetFound).isNotNull();
        assertThat(assetFound).usingRecursiveComparison().isEqualTo(asset);
    }

    @Test
    @DisplayName("Update an Asset that exists, removing a property")
    void updateAsset_exists_removesProperty() {
        var id = "id1";
        var asset = getAsset(id);
        asset.getProperties().put("newKey", "newValue");
        var assetIndex = getAssetIndex();
        assetIndex.create(asset);

        assertThat(assetIndex.countAssets(List.of())).isEqualTo(1);

        asset.getProperties().remove("newKey");
        var updated = assetIndex.updateAsset(asset);

        Assertions.assertThat(updated).isNotNull();

        var assetFound = getAssetIndex().findById("id1");

        assertThat(assetFound).isNotNull();
        assertThat(assetFound).usingRecursiveComparison().isEqualTo(asset);
        assertThat(assetFound.getProperties().keySet()).doesNotContain("newKey");
    }

    @Test
    @DisplayName("Update an Asset that exists, replacing a property")
    void updateAsset_exists_replacingProperty() {
        var id = "id1";
        var asset = getAsset(id);
        asset.getProperties().put("newKey", "originalValue");
        var assetIndex = getAssetIndex();
        assetIndex.create(asset);

        assertThat(assetIndex.countAssets(List.of())).isEqualTo(1);

        asset.getProperties().put("newKey", "newValue");
        var updated = assetIndex.updateAsset(asset);

        Assertions.assertThat(updated).isNotNull();

        var assetFound = getAssetIndex().findById("id1");

        assertThat(assetFound).isNotNull();
        assertThat(assetFound).usingRecursiveComparison().isEqualTo(asset);
        assertThat(assetFound.getProperties()).containsEntry("newKey", "newValue");
    }

    @Test
    @DisplayName("Update DataAddress where the Asset does not yet exist")
    void updateDataAddress_doesNotExist() {
        var id = "id1";
        var assetExpected = getDataAddress();
        var assetIndex = getAssetIndex();

        var updated = assetIndex.updateDataAddress(id, assetExpected);
        Assertions.assertThat(updated).isNotNull().extracting(StoreResult::reason).isEqualTo(NOT_FOUND);
    }

    @Test
    @DisplayName("Update a DataAddress that exists, adding a new property")
    void updateDataAddress_exists_addsProperty() {
        var id = "id1";
        var asset = getAsset(id);
        var assetIndex = getAssetIndex();
        assetIndex.create(asset);

        var updatedDataAddress = getDataAddress();
        updatedDataAddress.getProperties().put("newKey", "newValue");
        var updated = assetIndex.updateDataAddress(id, updatedDataAddress);

        Assertions.assertThat(updated).isNotNull();

        var addressFound = getAssetIndex().resolveForAsset("id1");

        assertThat(addressFound).isNotNull();
        assertThat(addressFound).usingRecursiveComparison().isEqualTo(updatedDataAddress);
    }

    @Test
    @DisplayName("Update a DataAddress that exists, removing a property")
    void updateDataAddress_exists_removesProperty() {
        var id = "id1";
        var asset = getAsset(id);
        var assetIndex = getAssetIndex();
        var dataAddress = getDataAddress();
        dataAddress.getProperties().put("newKey", "newValue");
        assetIndex.create(asset);

        var updatedDataAddress = dataAddress;
        updatedDataAddress.getProperties().remove("newKey");
        var updated = assetIndex.updateDataAddress(id, updatedDataAddress);

        Assertions.assertThat(updated).isNotNull();

        var addressFound = getAssetIndex().resolveForAsset("id1");

        assertThat(addressFound).isNotNull();
        assertThat(addressFound).usingRecursiveComparison().isEqualTo(updatedDataAddress);
        assertThat(addressFound.getProperties()).doesNotContainKeys("newKey");
    }

    @Test
    @DisplayName("Update a DataAddress that exists, replacing a property")
    void updateDataAddress_exists_replacesProperty() {
        var id = "id1";
        var asset = getAsset(id);
        var assetIndex = getAssetIndex();
        var dataAddress = getDataAddress();
        dataAddress.getProperties().put("newKey", "originalValue");
        assetIndex.create(asset);

        dataAddress.getProperties().put("newKey", "newValue");
        var updated = assetIndex.updateDataAddress(id, dataAddress);

        Assertions.assertThat(updated).isNotNull();

        var addressFound = getAssetIndex().resolveForAsset("id1");

        assertThat(addressFound).isNotNull();
        assertThat(addressFound).usingRecursiveComparison().isEqualTo(dataAddress);
        assertThat(addressFound.getProperties()).containsEntry("newKey", "newValue");
    }

    @NotNull
    protected Asset createAsset(String name) {
        return createAsset(name, UUID.randomUUID().toString());
    }

    @NotNull
    protected Asset createAsset(String name, String id) {
        return createAsset(name, id, "contentType");
    }

    @NotNull
    protected Asset createAsset(String name, String id, String contentType) {
        return Asset.Builder.newInstance()
                .id(id)
                .name(name)
                .version("1")
                .contentType(contentType)
                .dataAddress(DataAddress.Builder.newInstance()
                        .keyName("test-keyname")
                        .type(contentType)
                        .build())
                .build();
    }

    /**
     * Returns an array of all operators supported by a particular AssetIndex. If no limitations or constraints exist
     * (i.e. the AssetIndex supports all operators), then an empty list can be returned. Note that the operators MUST be
     * lowercase!
     */
    protected Collection<String> getSupportedOperators() {
        return emptyList();
    }

    /**
     * Returns the SuT i.e. the fully constructed instance of the {@link AssetIndex}
     */
    protected abstract AssetIndex getAssetIndex();

    protected DataAddress createDataAddress() {
        return DataAddress.Builder.newInstance()
                .keyName("test-keyname")
                .type("type")
                .build();
    }

    private QuerySpec filter(Criterion... criteria) {
        return QuerySpec.Builder.newInstance().filter(Arrays.asList(criteria)).build();
    }

    private Asset getAsset(String id) {
        return createAssetBuilder(id)
                .build();
    }

    protected Asset.Builder createAssetBuilder(String id) {
        return Asset.Builder.newInstance()
                .id(id)
                .createdAt(Clock.systemUTC().millis())
                .property("key" + id, "value" + id)
                .contentType("type")
                .dataAddress(getDataAddress());
    }

    private DataAddress getDataAddress() {
        return DataAddress.Builder.newInstance()
                .type("type")
                .property("key", "value")
                .build();
    }

}

