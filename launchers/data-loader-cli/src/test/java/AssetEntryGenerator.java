import org.eclipse.dataspaceconnector.dataloading.AssetEntry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Disabled
public class AssetEntryGenerator {

    @Test
    void createAssetEntries() throws IOException {
        var entries = IntStream.range(0, 10).mapToObj(i -> new AssetEntry(createAsset(i), createDataAddress(i)))
                .collect(Collectors.toUnmodifiableList());
        var json = new TypeManager().getMapper().writeValueAsString(entries);
        Files.write(Path.of("/home/paul/Documents/assets.json"), json.getBytes(StandardCharsets.UTF_8));
    }

    private DataAddress createDataAddress(int i) {
        return DataAddress.Builder.newInstance()
                .type("test-dataaddress-" + i)
                .property("someprop", "someval")
                .build();
    }

    private Asset createAsset(int i) {
        return Asset.Builder.newInstance()
                .id("test-asset-" + i)
                .name("test-asset-" + i)
                .version("1.0")
                .build();
    }
}
