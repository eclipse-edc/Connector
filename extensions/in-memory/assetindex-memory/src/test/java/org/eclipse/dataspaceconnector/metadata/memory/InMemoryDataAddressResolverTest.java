package org.eclipse.dataspaceconnector.metadata.memory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.easymock.EasyMock.niceMock;

class InMemoryDataAddressResolverTest {
    private InMemoryAssetIndex resolver;


    @BeforeEach
    void setUp() {
        resolver = new InMemoryAssetIndex(niceMock(Monitor.class), new CriterionToPredicateConverter());
    }

    @Test
    void resolveForAsset() {
        String id = UUID.randomUUID().toString();
        var testAsset = createAsset("foobar", id);
        DataAddress address = createDataAddress(testAsset);
        resolver.insert(testAsset, address);

        Assertions.assertThat(resolver.resolveForAsset(testAsset.getId())).isEqualTo(address);
    }

    @Test
    void resolveForAsset_assetNull_raisesException() {
        String id = UUID.randomUUID().toString();
        var testAsset = createAsset("foobar", id);
        DataAddress address = createDataAddress(testAsset);
        resolver.insert(testAsset, address);

        assertThatThrownBy(() -> resolver.resolveForAsset(null)).isInstanceOf(NullPointerException.class);
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
