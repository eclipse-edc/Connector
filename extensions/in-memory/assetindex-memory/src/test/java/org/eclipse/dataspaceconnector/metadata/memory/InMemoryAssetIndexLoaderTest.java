package org.eclipse.dataspaceconnector.metadata.memory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndexLoader;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.easymock.EasyMock.niceMock;

// this test aims at testing the thread-safety of the asset index
public class InMemoryAssetIndexLoaderTest {

    private final Random random = new Random();
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
        assertThatThrownBy(() -> assetIndexLoader.insert(null, niceMock(DataAddress.class))).isInstanceOf(NullPointerException.class);
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


    @BeforeEach
    void setup() {
        Monitor monitorMock = niceMock(Monitor.class);
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
