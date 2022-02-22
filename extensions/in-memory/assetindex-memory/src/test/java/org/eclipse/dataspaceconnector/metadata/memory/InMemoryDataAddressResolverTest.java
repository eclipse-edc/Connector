package org.eclipse.dataspaceconnector.metadata.memory;

import org.assertj.core.api.Assertions;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryDataAddressResolverTest {
    private InMemoryAssetIndex resolver;


    @BeforeEach
    void setUp() {
        resolver = new InMemoryAssetIndex(new AssetPredicateConverter());
    }

    @Test
    void resolveForAsset() {
        String id = UUID.randomUUID().toString();
        var testAsset = createAsset("foobar", id);
        DataAddress address = createDataAddress(testAsset);
        resolver.accept(testAsset, address);

        Assertions.assertThat(resolver.resolveForAsset(testAsset.getId())).isEqualTo(address);
    }

    @Test
    void resolveForAsset_assetNull_raisesException() {
        String id = UUID.randomUUID().toString();
        var testAsset = createAsset("foobar", id);
        DataAddress address = createDataAddress(testAsset);
        resolver.accept(testAsset, address);

        assertThatThrownBy(() -> resolver.resolveForAsset(null)).isInstanceOf(NullPointerException.class);
    }

    private Asset createAsset(String name, String id) {
        return Asset.Builder.newInstance().id(id).name(name).version("1").contentType("type").build();
    }

    private DataAddress createDataAddress(Asset asset) {
        return DataAddress.Builder.newInstance()
                .keyName("test-keyname")
                .type(asset.getContentType())
                .build();
    }
}
