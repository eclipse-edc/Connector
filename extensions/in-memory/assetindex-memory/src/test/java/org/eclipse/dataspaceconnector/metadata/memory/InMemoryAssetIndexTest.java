package org.eclipse.dataspaceconnector.metadata.memory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.easymock.EasyMock.niceMock;

class InMemoryAssetIndexTest {
    private InMemoryAssetIndex index;


    @BeforeEach
    void setUp() {
        index = new InMemoryAssetIndex(niceMock(Monitor.class), new EqualsOnlyPredicateFactory());
    }

    @Test
    void queryAssets() {
        var testAsset = Asset.Builder.newInstance().id(UUID.randomUUID().toString()).name("foobar").version("1").build();
        index.add(testAsset, createDataAddress(testAsset));
        var assets = index.queryAssets(AssetSelectorExpression.Builder.newInstance().whenEquals("name", "foobar").build());
        assertThat(assets).hasSize(1).containsExactly(testAsset);
    }

    @Test
    void queryAssets_notFound() {
        var testAsset = Asset.Builder.newInstance().id(UUID.randomUUID().toString()).name("foobar").version("1").build();
        index.add(testAsset, createDataAddress(testAsset));
        var assets = index.queryAssets(AssetSelectorExpression.Builder.newInstance().whenEquals("name", "barbaz").build());
        assertThat(assets).isEmpty();
    }

    @Test
    void queryAssets_fieldNull() {
        var testAsset = Asset.Builder.newInstance().id(UUID.randomUUID().toString()).name("foobar").version("1").build();
        index.add(testAsset, createDataAddress(testAsset));
        var assets = index.queryAssets(AssetSelectorExpression.Builder.newInstance().whenEquals("description", "barbaz").build());
        assertThat(assets).isEmpty();
    }

    @Test
    void queryAssets_multipleFound() {
        var testAsset1 = Asset.Builder.newInstance().id(UUID.randomUUID().toString()).name("foobar").version("1").build();
        var testAsset2 = Asset.Builder.newInstance().id(UUID.randomUUID().toString()).name("barbaz").version("1").build();
        var testAsset3 = Asset.Builder.newInstance().id(UUID.randomUUID().toString()).name("barbaz").version("1").build();
        index.add(testAsset1, createDataAddress(testAsset1));
        index.add(testAsset2, createDataAddress(testAsset2));
        index.add(testAsset3, createDataAddress(testAsset3));
        var assets = index.queryAssets(AssetSelectorExpression.Builder.newInstance()
                .whenEquals("name", "barbaz")
                .whenEquals("version", "1")
                .build());
        assertThat(assets).hasSize(2).containsExactlyInAnyOrder(testAsset2, testAsset3);
    }

    @Test
    void findById() {
        String id = UUID.randomUUID().toString();
        var testAsset = Asset.Builder.newInstance().id(id).name("foobar").version("1").build();
        index.add(testAsset, createDataAddress(testAsset));

        assertThat(index.findById(id)).isNotNull().isEqualTo(testAsset);
    }

    @Test
    void findById_notfound() {
        String id = UUID.randomUUID().toString();
        var testAsset = Asset.Builder.newInstance().id(id).name("foobar").version("1").build();
        index.add(testAsset, createDataAddress(testAsset));

        assertThat(index.findById("not-exist")).isNull();
    }

    @Test
    void findById_multiple_raisesException() {
        String id = UUID.randomUUID().toString();
        var testAsset = Asset.Builder.newInstance().id(id).name("foobar").version("1").build();
        var testAsset2 = Asset.Builder.newInstance().id(id).name("foobar").version("1").build();

        index.add(testAsset, createDataAddress(testAsset));
        index.add(testAsset2, createDataAddress(testAsset2)); //in an actual DB this would likely cause problems already

        assertThatThrownBy(() -> index.findById(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resolveForAsset() {
        String id = UUID.randomUUID().toString();
        var testAsset = Asset.Builder.newInstance().id(id).name("foobar").version("1").build();
        DataAddress address = createDataAddress(testAsset);
        index.add(testAsset, address);

        assertThat(index.resolveForAsset(testAsset.getId())).isEqualTo(address);
    }

    @Test
    void resolveForAsset_notFound_raisesIllegalArgException() {
        String id = UUID.randomUUID().toString();
        var testAsset = Asset.Builder.newInstance().id(id).name("foobar").version("1").build();
        index.add(testAsset, null);

        assertThatThrownBy(() -> index.resolveForAsset(testAsset.getId())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveForAsset_assetNull_raisesException() {
        String id = UUID.randomUUID().toString();
        var testAsset = Asset.Builder.newInstance().id(id).name("foobar").version("1").build();
        DataAddress address = createDataAddress(testAsset);
        index.add(testAsset, address);

        assertThatThrownBy(() -> index.resolveForAsset(null)).isInstanceOf(NullPointerException.class);
    }

    private DataAddress createDataAddress(Asset asset) {
        return DataAddress.Builder.newInstance()
                .type("test-asset")
                .keyName("test-keyname")
                .properties(flatten(asset))
                .build();
    }

    private Map<String, ?> flatten(Object object) {

        try {
            var om = new ObjectMapper();
            om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            var json = om.writeValueAsString(object);
            return om.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
