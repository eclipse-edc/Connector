package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression.SELECT_ALL;

class InMemoryAssetIndexTest {
    private InMemoryAssetLoader index;

    @BeforeEach
    void setUp() {
        index = new InMemoryAssetLoader(new CriterionToPredicateConverter());
    }

    @Test
    void queryAssets() {
        var testAsset = createAsset("foobar");
        index.accept(testAsset, dataAddress());

        var assets = index.queryAssets(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_NAME, "foobar").build());

        assertThat(assets).hasSize(1).containsExactly(testAsset);
    }

    @Test
    void queryAssets_notFound() {
        var testAsset = createAsset("foobar");
        index.accept(testAsset, dataAddress());

        var assets = index.queryAssets(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_NAME, "barbaz").build());

        assertThat(assets).isEmpty();
    }

    @Test
    void queryAssets_fieldNull() {
        var testAsset = createAsset("foobar");
        index.accept(testAsset, dataAddress());

        var assets = index.queryAssets(AssetSelectorExpression.Builder.newInstance().whenEquals("description", "barbaz").build());

        assertThat(assets).isEmpty();
    }

    @Test
    void queryAssets_multipleFound() {
        var testAsset1 = createAsset("foobar");
        var testAsset2 = createAsset("barbaz");
        var testAsset3 = createAsset("barbaz");
        index.accept(testAsset1, dataAddress());
        index.accept(testAsset2, dataAddress());
        index.accept(testAsset3, dataAddress());

        var assets = index.queryAssets(AssetSelectorExpression.Builder.newInstance()
                .whenEquals(Asset.PROPERTY_NAME, "barbaz")
                .whenEquals(Asset.PROPERTY_VERSION, "1")
                .build());

        assertThat(assets).hasSize(2).containsExactlyInAnyOrder(testAsset2, testAsset3);
    }

    @Test
    void queryAssets_selectAll_shouldReturnAll() {
        var testAsset1 = createAsset("barbaz");
        index.accept(testAsset1, dataAddress());

        var testAsset2 = createAsset("foobar");
        index.accept(testAsset2, dataAddress());

        var results = index.queryAssets(SELECT_ALL);

        assertThat(results).containsExactlyInAnyOrder(testAsset1, testAsset2);
    }

    @Test
    void findById() {
        String id = UUID.randomUUID().toString();
        var testAsset = createAsset("barbaz", id);
        index.accept(testAsset, dataAddress());

        var result = index.findById(id);

        assertThat(result).isNotNull().isEqualTo(testAsset);
    }


    @Test
    void findById_notfound() {
        String id = UUID.randomUUID().toString();
        var testAsset = createAsset("foobar", id);
        index.accept(testAsset, dataAddress());

        var result = index.findById("not-exist");

        assertThat(result).isNull();
    }

    @Test
    void queryAsset_operatorIn() {
        var testAsset1 = createAsset("foobar");
        var testAsset2 = createAsset("barbaz");
        var testAsset3 = createAsset("barbaz");
        index.accept(testAsset1, dataAddress());
        index.accept(testAsset2, dataAddress());
        index.accept(testAsset3, dataAddress());

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
        index.accept(testAsset1, dataAddress());
        index.accept(testAsset2, dataAddress());
        index.accept(testAsset3, dataAddress());

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
        index.accept(testAsset1, dataAddress());
        index.accept(testAsset2, dataAddress());
        index.accept(testAsset3, dataAddress());

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
        index.accept(testAsset1, dataAddress());
        index.accept(testAsset2, dataAddress());
        index.accept(testAsset3, dataAddress());

        var inExpr = String.join(",", List.of(testAsset1.getId(), testAsset2.getId()));
        var selector = AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "IN", inExpr)
                .build();

        var assets = index.queryAssets(selector);

        assertThat(assets).hasSize(2).containsExactlyInAnyOrder(testAsset1, testAsset2);
    }

    @NotNull
    private DataAddress dataAddress() {
        return DataAddress.Builder.newInstance().build();
    }

    @NotNull
    private Asset createAsset(String name) {
        return createAsset(name, UUID.randomUUID().toString());
    }

    @NotNull
    private Asset createAsset(String name, String id) {
        return Asset.Builder.newInstance().id(id).name(name).version("1").build();
    }

}
