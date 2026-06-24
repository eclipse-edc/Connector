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

package org.eclipse.edc.connector.controlplane.asset.spi.testfixtures;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

/**
 * This is the minimum test specification that all {@link AssetIndex} implementations must support. All
 * {@link AssetIndex} tests, that actually utilize the target system (SQL, MongoDb,....) MUST inherit this class. Pure
 * unit tests need not inherit this, as they will likely heavily rely on mocks that require specific preparation.
 */
public abstract class AssetIndexTestBase {

    /**
     * Returns the SuT i.e. the fully constructed instance of the {@link AssetIndex}
     */
    protected abstract AssetIndex getAssetIndex();

    private Asset createAsset(String id) {
        return createAssetBuilder(id)
                .build();
    }

    private Asset.Builder createAssetBuilder(String id) {
        return createAssetBuilder()
                .id(id)
                .property("key" + id, "value" + id);
    }

    private Asset.Builder createAssetBuilder() {
        return Asset.Builder.newInstance()
                .createdAt(Clock.systemUTC().millis())
                .dataAddress(createDataAddress())
                .participantContextId("participantContextId")
                .dataplaneMetadata(DataplaneMetadata.Builder.newInstance().property("dataplanePropertyKey", "value").label("label").build());
    }

    private QuerySpec filter(Criterion... criteria) {
        return QuerySpec.Builder.newInstance().filter(Arrays.asList(criteria)).build();
    }

    private DataAddress createDataAddress() {
        return DataAddress.Builder.newInstance()
                .type("type")
                .property("key", "value")
                .build();
    }

    @Nested
    class Create {
        @Test
        void shouldStoreAsset() {
            var assetExpected = createAsset("id1");
            getAssetIndex().create(assetExpected);

            var assetFound = getAssetIndex().findById("id1");

            assertThat(assetFound).isNotNull();
            assertThat(assetFound).usingRecursiveComparison().isEqualTo(assetExpected);
            assertThat(assetFound.getCreatedAt()).isGreaterThan(0);
        }

        @Test
        void shouldFail_whenAssetAlreadyExists() {
            var asset = createAsset(UUID.randomUUID().toString());
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
        void shouldCreate_withPrivateProperty() {
            var asset = createAssetBuilder("test-asset").privateProperty("prop1", "val1")
                    .property(Asset.PROPERTY_IS_CATALOG, true)
                    .build();

            assertThat(getAssetIndex().create(asset).succeeded()).isTrue();
            var assetFound = getAssetIndex().findById(asset.getId());

            assertThat(assetFound).isNotNull();
            assertThat(assetFound.isCatalog()).isTrue();

        }
    }

    @Nested
    class DeleteById {

        @Test
        @DisplayName("Delete an asset that doesn't exist")
        void doesNotExist() {
            var assetDeleted = getAssetIndex().deleteById("id1");

            Assertions.assertThat(assetDeleted).isNotNull().extracting(StoreResult::reason).isEqualTo(NOT_FOUND);
        }

        @Test
        @DisplayName("Delete an asset that exists")
        void exists() {
            var asset = createAsset("id1");
            getAssetIndex().create(asset);

            var assetDeleted = getAssetIndex().deleteById("id1");

            assertThat(assetDeleted).isNotNull().extracting(StoreResult::succeeded).isEqualTo(true);
            assertThat(assetDeleted.getContent()).usingRecursiveComparison().isEqualTo(asset);

            assertThat(getAssetIndex().queryAssets(QuerySpec.none())).isEmpty();
        }
    }

    @Nested
    class CountAssets {
        @Test
        void withResults() {
            var assets = range(0, 5).mapToObj(i -> createAsset("id" + i));
            assets.forEach(a -> getAssetIndex().create(a));
            var criteria = Collections.<Criterion>emptyList();

            var count = getAssetIndex().countAssets(criteria);

            assertThat(count).isEqualTo(5);
        }

        @Test
        void withNoResults() {
            var criteria = Collections.<Criterion>emptyList();

            var count = getAssetIndex().countAssets(criteria);

            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    class QueryAssets {

        @Test
        void shouldReturnAllTheAssets_whenQuerySpecIsEmpty() {
            var assets = IntStream.range(0, 5)
                    .mapToObj(i -> createAsset("id" + i))
                    .peek(a -> getAssetIndex().create(a)).toList();

            var result = getAssetIndex().queryAssets(QuerySpec.none());

            assertThat(result).hasSize(5).usingRecursiveFieldByFieldElementComparator().containsAll(assets);
        }

        @Test
        @DisplayName("Query assets with query spec")
        void limit() {
            range(1, 10).mapToObj(it -> createAsset("id" + it)).forEach(asset -> getAssetIndex().create(asset));
            var querySpec = QuerySpec.Builder.newInstance().limit(3).offset(2).build();

            var assetsFound = getAssetIndex().queryAssets(querySpec);

            assertThat(assetsFound).isNotNull().hasSize(3);
        }

        @Test
        @DisplayName("Query assets with query spec and short asset count")
        void shortCount() {
            range(1, 5).mapToObj(it -> createAsset("id" + it)).forEach(asset -> getAssetIndex().create(asset));
            var querySpec = QuerySpec.Builder.newInstance()
                    .limit(3)
                    .offset(2)
                    .build();

            var assetsFound = getAssetIndex().queryAssets(querySpec);

            assertThat(assetsFound).isNotNull().hasSize(2);
        }

        @Test
        void shouldReturnNoAssets_whenOffsetIsOutOfBounds() {
            range(1, 5).mapToObj(it -> createAsset("id" + it)).forEach(asset -> getAssetIndex().create(asset));
            var querySpec = QuerySpec.Builder.newInstance()
                    .limit(3)
                    .offset(5)
                    .build();

            var assetsFound = getAssetIndex().queryAssets(querySpec);

            assertThat(assetsFound).isEmpty();
        }

        @Test
        void shouldThrowException_whenUnsupportedOperator() {
            var asset = createAsset("id1");
            getAssetIndex().create(asset);
            var unsupportedOperator = new Criterion(Asset.PROPERTY_ID, "unsupported", "42");

            assertThatThrownBy(() -> getAssetIndex().queryAssets(filter(unsupportedOperator)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldReturnEmpty_whenLeftOperandDoesNotExist() {
            var asset = createAsset("id1");
            getAssetIndex().create(asset);
            var notExistingProperty = new Criterion("noexist", "=", "42");

            var result = getAssetIndex().queryAssets(filter(notExistingProperty));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Query assets with query spec where the value (=rightOperand) does not exist")
        void nonExistValue() {
            var asset = createAsset("id1");
            asset.getProperties().put("someprop", "someval");
            getAssetIndex().create(asset);
            var notExistingValue = new Criterion("someprop", "=", "some-other-val");

            var assets = getAssetIndex().queryAssets(filter(notExistingValue));

            assertThat(assets).isEmpty();
        }

        @Test
        @DisplayName("Verifies an asset query, that contains a filter expression")
        void withFilterExpression() {
            var expected = createAssetBuilder("id1").property("version", "2.0").property("contentType", "whatever").build();
            var differentVersion = createAssetBuilder("id2").property("version", "2.1").property("contentType", "whatever").build();
            var differentContentType = createAssetBuilder("id3").property("version", "2.0").property("contentType", "different").build();
            getAssetIndex().create(expected);
            getAssetIndex().create(differentVersion);
            getAssetIndex().create(differentContentType);
            var filter = filter(
                    new Criterion("version", "=", "2.0"),
                    new Criterion("contentType", "=", "whatever")
            );

            var assets = getAssetIndex().queryAssets(filter);

            assertThat(assets).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsOnly(expected);
        }

        @Test
        void shouldFilterByNestedProperty() {
            var nested = EDC_NAMESPACE + "nested";
            var version = EDC_NAMESPACE + "version";
            var expected = createAssetBuilder("id1").property(nested, Map.of(version, "2.0")).build();
            var differentVersion = createAssetBuilder("id2").property(nested, Map.of(version, "2.1")).build();
            getAssetIndex().create(expected);
            getAssetIndex().create(differentVersion);

            var assets = getAssetIndex().queryAssets(filter(criterion("'%s'.'%s'".formatted(nested, version), "=", "2.0")));

            assertThat(assets).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsOnly(expected);
        }

        @Test
        void shouldFilterByParticipantContext() {
            var expected = createAssetBuilder("id1").participantContextId("context1").build();
            var differentVersion = createAssetBuilder("id2").participantContextId("context2").build();
            getAssetIndex().create(expected);
            getAssetIndex().create(differentVersion);

            var assets = getAssetIndex().queryAssets(filter(filterByParticipantContextId("context1")));

            assertThat(assets).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsOnly(expected);
        }

        @Test
        @DisplayName("Verify an asset query based on an Asset property, where the property value is actually a complex object")
        void assetPropertyAsObject() {
            var nested = Map.of("text", "test123", "number", 42, "bool", false);
            var asset = createAssetBuilder("id1")
                    .property("testobj", nested)
                    .build();
            getAssetIndex().create(asset);

            var assetsFound = getAssetIndex().queryAssets(QuerySpec.Builder.newInstance()
                    .filter(criterion("testobj", "like", "%test1%"))
                    .build());

            assertThat(assetsFound).hasSize(1).first().usingRecursiveComparison().isEqualTo(asset);
        }

        @Test
        void multipleFound() {
            var testAsset1 = createAssetBuilder(UUID.randomUUID().toString()).property("propertyKey", "foobar").build();
            var testAsset2 = createAssetBuilder(UUID.randomUUID().toString()).property("propertyKey", "barbaz").build();
            var testAsset3 = createAssetBuilder(UUID.randomUUID().toString()).property("propertyKey", "barbaz").build();
            getAssetIndex().create(testAsset1);
            getAssetIndex().create(testAsset2);
            getAssetIndex().create(testAsset3);
            var criterion = new Criterion("propertyKey", "=", "barbaz");

            var assets = getAssetIndex().queryAssets(filter(criterion));

            assertThat(assets).hasSize(2).map(Asset::getId).containsExactlyInAnyOrder(testAsset2.getId(), testAsset3.getId());
        }

        @Test
        @DisplayName("Query assets using the IN operator")
        void in() {
            getAssetIndex().create(createAsset("id1"));
            getAssetIndex().create(createAsset("id2"));
            var criterion = new Criterion(Asset.PROPERTY_ID, "in", List.of("id1", "id2"));

            var assetsFound = getAssetIndex().queryAssets(filter(criterion));

            assertThat(assetsFound).isNotNull().hasSize(2);
        }

        @Test
        @DisplayName("Query assets using the IN operator, invalid right operand")
        void shouldThrowException_whenOperatorInAndInvalidRightOperand() {
            var asset1 = createAsset("id1");
            getAssetIndex().create(asset1);
            var asset2 = createAsset("id2");
            getAssetIndex().create(asset2);
            var invalidRightOperand = new Criterion(Asset.PROPERTY_ID, "in", "(id1, id2)");

            assertThatThrownBy(() -> getAssetIndex().queryAssets(filter(invalidRightOperand)).toList())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldSortByProperty() {
            var assets = IntStream.range(9, 12)
                    .mapToObj(i -> createAsset("id" + i))
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
        void shouldSortByPrivateProperty() {
            var assets = IntStream.range(0, 10)
                    .mapToObj(i -> createAssetBuilder(String.valueOf(i)).privateProperty("pKey", "pValue").build())
                    .peek(a -> getAssetIndex().create(a))
                    .toList();

            var spec = QuerySpec.Builder.newInstance().sortField("pKey").sortOrder(SortOrder.ASC).build();

            var result = getAssetIndex().queryAssets(spec);

            assertThat(result).usingRecursiveFieldByFieldElementComparator().containsAll(assets);
        }

        @Test
        void shouldSortByCreatedAt() {
            var assets = IntStream.range(0, 10)
                    .mapToObj(i -> createAssetBuilder(String.valueOf(i)).privateProperty("pKey", "pValue").build())
                    .peek(a -> getAssetIndex().create(a))
                    .toList();

            var spec = QuerySpec.Builder.newInstance().sortField("createdAt").sortOrder(SortOrder.DESC).build();

            var result = getAssetIndex().queryAssets(spec);

            var reversedAssets = new ArrayList<>(assets);
            Collections.reverse(reversedAssets);
            assertThat(result).usingRecursiveFieldByFieldElementComparator().containsAll(reversedAssets);
        }

        @Test
        void shouldFilter_whenLikeOperator() {
            var asset1 = createAsset("id1");
            getAssetIndex().create(asset1);
            var asset2 = createAsset("id2");
            getAssetIndex().create(asset2);
            var criterion = new Criterion(Asset.PROPERTY_ID, "LIKE", "id%");

            var assetsFound = getAssetIndex().queryAssets(filter(criterion));

            assertThat(assetsFound).isNotNull().hasSize(2);
        }

        @Test
        void shouldFilter_whenIlikeOperator() {
            getAssetIndex().create(createAsset("ID1"));
            getAssetIndex().create(createAsset("ID2"));
            var criterion = new Criterion(Asset.PROPERTY_ID, "ilike", "id%");

            var assetsFound = getAssetIndex().queryAssets(filter(criterion));

            assertThat(assetsFound).isNotNull().hasSize(2);
        }

        @Test
        @DisplayName("Query assets using the LIKE operator on a json value")
        void likeJson() throws JsonProcessingException {
            var asset = createAsset("id1");
            var nested = Map.of("text", "test123", "number", 42, "bool", false);
            asset.getProperties().put("myjson", new ObjectMapper().writeValueAsString(nested));
            getAssetIndex().create(asset);
            var criterion = new Criterion("myjson", "LIKE", "%test123%");

            var assetsFound = getAssetIndex().queryAssets(filter(criterion));

            assertThat(assetsFound).usingRecursiveFieldByFieldElementComparator().containsExactly(asset);
        }

        @Test
        @DisplayName("Query assets using two criteria, each with the LIKE operator on a nested json value")
        void likeJson_withComplexObject() throws JsonProcessingException {
            var asset = createAsset("id1");
            var jsonObject = Map.of("root", Map.of("key1", "value1", "nested1", Map.of("key2", "value2", "key3", Map.of("theKey", "theValue, this is what we're looking for"))));
            asset.getProperties().put("myProp", new ObjectMapper().writeValueAsString(jsonObject));
            getAssetIndex().create(asset);
            var criterion1 = new Criterion("myProp", "LIKE", "%is%what%");
            var criterion2 = new Criterion("myProp", "LIKE", "%we're%looking%");

            var assetsFound = getAssetIndex().queryAssets(filter(criterion1, criterion2));

            assertThat(assetsFound).usingRecursiveFieldByFieldElementComparator().containsExactly(asset);
        }
    }

    @Nested
    class FindById {
        @Test
        void shouldReturnAsset() {
            var id = UUID.randomUUID().toString();
            var asset = createAsset(id);
            getAssetIndex().create(asset);

            var assetFound = getAssetIndex().findById(id);

            assertThat(assetFound).isNotNull();
            assertThat(assetFound).usingRecursiveComparison().isEqualTo(asset);
        }

        @Test
        void shouldReturnNull_whenAssetDoesNotExist() {
            var result = getAssetIndex().findById("unexistent");

            assertThat(result).isNull();
        }
    }

    @Nested
    class ResolveForAsset {
        @Test
        @DisplayName("Find a data address that doesn't exist")
        void doesNotExist() {
            assertThat(getAssetIndex().resolveForAsset("id1")).isNull();
        }

        @Test
        @DisplayName("Find a data address that exists")
        void exists() {
            var asset = createAsset("id1");
            var dataAddress = createDataAddress();
            getAssetIndex().create(asset);

            var dataAddressFound = getAssetIndex().resolveForAsset("id1");

            assertThat(dataAddressFound).isNotNull();
            assertThat(dataAddressFound).usingRecursiveComparison().isEqualTo(dataAddress);
        }
    }

    @Nested
    class UpdateAsset {
        @Test
        @DisplayName("Update Asset that does not yet exist")
        void doesNotExist() {
            var id = "id1";
            var assetExpected = createAsset(id);
            var assetIndex = getAssetIndex();

            var updated = assetIndex.updateAsset(assetExpected);
            Assertions.assertThat(updated).isNotNull().extracting(StoreResult::succeeded).isEqualTo(false);
        }

        @Test
        @DisplayName("Update an Asset that exists, adding a property")
        void exists_addsProperty() {
            var id = "id1";
            var asset = createAsset(id);
            var assetIndex = getAssetIndex();
            assetIndex.create(asset);

            assertThat(assetIndex.countAssets(List.of())).isEqualTo(1);

            var newAsset = createAsset(id);
            newAsset.getProperties().put("newKey", "newValue");

            var updated = assetIndex.updateAsset(newAsset);

            Assertions.assertThat(updated).isNotNull();

            var assetFound = getAssetIndex().findById("id1");

            assertThat(assetFound).isNotNull();
            assertThat(assetFound)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(newAsset);
            assertThat(assetFound.getProperties()).containsEntry("newKey", "newValue");
        }

        @Test
        @DisplayName("Update an Asset that exists, removing a property")
        void exists_removesProperty() {
            var id = "id1";
            var asset = createAsset(id);
            asset.getProperties().put("newKey", "newValue");
            var assetIndex = getAssetIndex();
            assetIndex.create(asset);

            assertThat(assetIndex.countAssets(List.of())).isEqualTo(1);

            var newAsset = createAsset(id);
            newAsset.getProperties().remove("newKey");

            var updated = assetIndex.updateAsset(newAsset);

            Assertions.assertThat(updated).isNotNull();

            var assetFound = getAssetIndex().findById("id1");

            assertThat(assetFound).isNotNull();
            assertThat(assetFound)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(newAsset);
            assertThat(assetFound.getProperties().keySet()).doesNotContain("newKey");
        }

        @Test
        @DisplayName("Update an Asset that exists, replacing a property")
        void exists_replacingProperty() {
            var id = "id1";
            var asset = createAsset(id);
            asset.getProperties().put("newKey", "originalValue");
            var assetIndex = getAssetIndex();
            assetIndex.create(asset);

            assertThat(assetIndex.countAssets(List.of())).isEqualTo(1);

            var newAsset = createAsset(id);
            newAsset.getProperties().put("newKey", "newValue");
            var updated = assetIndex.updateAsset(newAsset);

            Assertions.assertThat(updated).isNotNull();

            var assetFound = getAssetIndex().findById("id1");

            assertThat(assetFound).isNotNull();
            assertThat(assetFound)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(newAsset);
            assertThat(assetFound.getProperties()).containsEntry("newKey", "newValue");
        }

        @Test
        void exists_updateDataAddress() {
            var id = "id1";
            var asset = createAsset(id);
            var assetIndex = getAssetIndex();
            assetIndex.create(asset);

            assertThat(assetIndex.countAssets(List.of())).isEqualTo(1);

            var newAsset = createAsset(id);
            newAsset.getDataAddress().getProperties().put("newKey", "newValue");
            var updated = assetIndex.updateAsset(newAsset);

            Assertions.assertThat(updated).isNotNull();

            var assetFound = assetIndex.findById("id1");
            var dataAddressFound = assetIndex.resolveForAsset("id1");

            assertThat(assetFound).isNotNull();
            assertThat(dataAddressFound).isNotNull();
            assertThat(assetFound)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(newAsset);
            assertThat(assetFound.getDataAddress())
                    .usingRecursiveComparison()
                    .isEqualTo(dataAddressFound);
        }
    }

}

