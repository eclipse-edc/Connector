package org.eclipse.dataspaceconnector.metadata.memory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndexLoader;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.niceMock;

// this test aims at testing the thread-safety of the asset index
public class InMemoryAssetIndexConsistencyTest {

    private final Random random = new Random();
    private AssetIndex assetIndex;
    private AssetIndexLoader assetIndexLoader;

    @Test
    void multipleReadsWrites() throws InterruptedException {
        var latch = new CountDownLatch(20);
        Runnable insertTask = () -> {
            Asset asset = createAsset("test-asset", UUID.randomUUID().toString());
            assetIndexLoader.insert(asset, createDataAddress(asset));
            System.out.println("inserted!");
            latch.countDown();

        };

        Callable<Stream<Asset>> readTask = () -> {
            var stream = assetIndex.queryAssets(AssetSelectorExpression.SELECT_ALL);
            latch.countDown();
            System.out.println("got " + stream.count());
            return stream;
        };

        var scheduler = Executors.newScheduledThreadPool(3);
        IntStream.range(0, 10).forEach(i -> scheduler.schedule(insertTask, 500 + random.nextInt(1000), TimeUnit.MILLISECONDS));
        IntStream.range(0, 10).forEach(i -> scheduler.schedule(readTask, 500 + random.nextInt(1000), TimeUnit.MILLISECONDS));

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

    }

    @BeforeEach
    void setup() {
        Monitor monitorMock = niceMock(Monitor.class);
        var index = new InMemoryAssetIndex(monitorMock, new CriterionToPredicateConverter());
        assetIndex = index;
        assetIndexLoader = index;
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
