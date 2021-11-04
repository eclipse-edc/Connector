package org.eclipse.dataspaceconnector.metadata.memory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndexLoader;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InMemoryAssetIndexLoaderTest {

    private AssetIndexLoader assetIndexLoader;

    @Test
    void insert() {
        var asset = createAsset("test-asset", UUID.randomUUID().toString());
        var dataAddress = createDataAddress(asset);
        assetIndexLoader.insert(asset, dataAddress);

        assertThat(((InMemoryAssetIndex) assetIndexLoader).getAssets()).hasSize(1);
        assertThat(((InMemoryAssetIndex) assetIndexLoader).getDataAddresses()).hasSize(1);
    }

    @Test
    void insert_illegalParams() {
        var dataAddress = DataAddress.Builder.newInstance().build();
        assertThatThrownBy(() -> assetIndexLoader.insert(null, dataAddress)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> assetIndexLoader.insert(createAsset("testasset", "testid"), null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void insert_exists() {
        var asset = createAsset("test-asset", UUID.randomUUID().toString());
        var dataAddress = createDataAddress(asset);
        assetIndexLoader.insert(asset, dataAddress);
        DataAddress dataAddress1 = createDataAddress(asset);
        assetIndexLoader.insert(asset, dataAddress1);

        //assert that this replaces the previous data address
        assertThat(((InMemoryAssetIndex) assetIndexLoader).getAssets()).hasSize(1).containsValue(asset);
        assertThat(((InMemoryAssetIndex) assetIndexLoader).getDataAddresses()).hasSize(1).containsValue(dataAddress1);

    }

    @Test
    void insertAll() {
        var asset1 = createAsset("asset1", "id1");
        var asset2 = createAsset("asset2", "id2");

        var address1 = createDataAddress(asset1);
        var address2 = createDataAddress(asset2);

        assetIndexLoader.insertAll(List.of(new AbstractMap.SimpleEntry<>(asset1, address1), new AbstractMap.SimpleEntry<>(asset2, address2)));

        assertThat(((InMemoryAssetIndex) assetIndexLoader).getAssets()).hasSize(2);
        assertThat(((InMemoryAssetIndex) assetIndexLoader).getDataAddresses()).hasSize(2);

    }

    @Test
    void insertAll_oneExists_shouldOverwrite() {
        var asset1 = createAsset("asset1", "id1");

        var address1 = createDataAddress(asset1);
        var address2 = createDataAddress(asset1);

        assetIndexLoader.insertAll(List.of(new AbstractMap.SimpleEntry<>(asset1, address1), new AbstractMap.SimpleEntry<>(asset1, address2)));

        // only one address/asset combo should exist
        assertThat(((InMemoryAssetIndex) assetIndexLoader).getAssets()).hasSize(1);
        assertThat(((InMemoryAssetIndex) assetIndexLoader).getDataAddresses()).hasSize(1).containsValue(address2).containsKeys(asset1.getId());

    }

    @BeforeEach
    void setup() {
        assetIndexLoader = new InMemoryAssetIndex(new CriterionToPredicateConverter());
    }

    private Asset createAsset(String name, String id) {
        return Asset.Builder.newInstance().id(id).name(name).version("1").build();
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
