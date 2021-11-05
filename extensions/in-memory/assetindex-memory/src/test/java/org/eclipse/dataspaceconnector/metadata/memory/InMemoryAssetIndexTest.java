package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndexQuery;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.pagination.Cursor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAssetIndexTest {
    private InMemoryAssetIndex index;
    private InMemoryAssetStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryAssetStorage();
        index = new InMemoryAssetIndex(storage, new CriterionToPredicateConverter());
    }

    @Test
    void queryAssets() {
        var testAsset = createAsset("foobar");
        storage.add(testAsset);
        var expression = AssetSelectorExpression.Builder.newInstance().whenEquals("name", "foobar").build();
        var assets = index.queryAssets(createQuery(expression));
        assertThat(assets).hasSize(1).containsExactly(testAsset);
    }

    @Test
    void queryAssets_notFound() {
        var testAsset = createAsset("foobar");
        storage.add(testAsset);
        var expression = AssetSelectorExpression.Builder.newInstance().whenEquals("name", "barbaz").build();
        var assets = index.queryAssets(createQuery(expression));
        assertThat(assets).isEmpty();
    }

    @Test
    void queryAssets_fieldNull() {
        var testAsset = createAsset("foobar");
        storage.add(testAsset);
        var expression = AssetSelectorExpression.Builder.newInstance().whenEquals("description", "barbaz").build();
        var assets = index
                .queryAssets(createQuery(expression));
        assertThat(assets).isEmpty();
    }

    @Test
    void queryAssets_multipleFound() {
        var testAsset1 = createAsset("foobar");
        var testAsset2 = createAsset("barbaz");
        var testAsset3 = createAsset("barbaz");
        storage.add(testAsset1);
        storage.add(testAsset2);
        storage.add(testAsset3);
        var expression = AssetSelectorExpression.Builder.newInstance()
                .whenEquals("name", "barbaz")
                .whenEquals("version", "1")
                .build();
        var assets = index
                .queryAssets(createQuery(expression));
        assertThat(assets).hasSize(2).containsExactlyInAnyOrder(testAsset2, testAsset3);
    }

    @Test
    void queryAssets_noExpression_shouldReturnEmpty() {
        var expression = AssetSelectorExpression.Builder.newInstance().build();
        var result = index
                .queryAssets(createQuery(expression));
        assertThat(result).isEmpty();
    }

    @Test
    void queryAssets_selectAll_shouldReturnAll() {
        var testAsset1 = createAsset("barbaz");
        storage.add(testAsset1);

        var testAsset2 = createAsset("foobar");
        storage.add(testAsset2);

        var assets = index.queryAssets(createQuery(AssetSelectorExpression.SELECT_ALL));
        assertThat(assets).containsExactlyInAnyOrder(testAsset1, testAsset2);
    }

    @Test
    void findById() {
        String id = UUID.randomUUID().toString();
        var testAsset = createAsset("barbaz", id);
        storage.add(testAsset);

        assertThat(index.findById(id)).isNotNull().isEqualTo(testAsset);
    }


    @Test
    void findById_notfound() {
        var testAsset = createAsset("foobar");
        storage.add(testAsset);

        assertThat(index.findById("not-exist")).isNull();
    }


    @Test
    void queryAllAssetsWithCursor() {
        int numberOfAssets = 20;
        for (var i = 0; i < numberOfAssets; i++) {
            var testAsset = createAsset("asset-" + i, String.format("%d", i));
            storage.add(testAsset);
        }

        var expression = AssetSelectorExpression.SELECT_ALL;

        int assetsQueried = 0;
        Cursor cursor = null;
        do {
            var query = AssetIndexQuery.Builder.newInstance().expression(expression).limit(3).nextCursor(cursor).build();
            var resultSet1 = index.queryAssets(query);
            for (var ignored : resultSet1) {
                assetsQueried += 1;
            }
            cursor = resultSet1.getNextCursor();

        } while (cursor != null);

        Assertions.assertEquals(numberOfAssets, assetsQueried);
    }

    @NotNull
    private Asset createAsset(String name) {
        return createAsset(name, UUID.randomUUID().toString());
    }

    private Asset createAsset(String name, String id) {
        return Asset.Builder.newInstance().id(id).name(name).version("1").build();
    }

    private AssetIndexQuery createQuery(@NotNull AssetSelectorExpression expression) {
        return AssetIndexQuery.Builder.newInstance().expression(expression).limit(100).build();
    }
}
