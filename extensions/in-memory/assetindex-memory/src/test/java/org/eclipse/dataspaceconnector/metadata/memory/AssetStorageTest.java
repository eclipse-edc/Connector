package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;

public class AssetStorageTest {

    private AssetStorage storage;

    @BeforeEach
    public void setup() {

        var storage = new InMemoryAssetStorage();
        // insert assets from Asset:0 to Asset:9
        for (var i = 0; i < 10; i++) {
            storage.add(Asset.Builder.newInstance()
                    .id(String.format("Asset:%s", i))
                    .build());
        }

        this.storage = storage;
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 3, 5, 15, 19 })
    public void testRetrieveAssetById(int index) {
        var asset = storage.getAsset(String.format("Asset:%s", index));
        Assertions.assertNotNull(asset);
    }

    @Test
    public void testRetrieveAssetsAscending() {
        // prepare
        var assets = new ArrayList<Asset>();

        // invoke
        var iterator = storage.getAssetsAscending("Asset:5");
        iterator.forEachRemaining(assets::add);

        // verify [Asset:6, Asset:7, Asset:8, Asset:9]
        Assertions.assertEquals(4, assets.size());
        Assertions.assertEquals("Asset:6", assets.get(0).getId());
        Assertions.assertEquals("Asset:7", assets.get(1).getId());
        Assertions.assertEquals("Asset:8", assets.get(2).getId());
        Assertions.assertEquals("Asset:9", assets.get(3).getId());
    }

}
