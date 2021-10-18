package org.eclipse.dataspaceconnector.catalog.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EdcExtension.class)
class FederatedCatalogCacheEndToEndTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void test(FederatedCacheStore store, OkHttpClient client) throws IOException {
        int nbAssets = 3;

        // generate assets and populate the store
        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < nbAssets; i++) {
            assets.add(buildAsset());
        }
        assets.forEach(store::save);

        // here the content of the catalog cache store can be queried through http://localhost:8181/api/catalog
        RequestBody body = RequestBody.create("{}", MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("http://localhost:8181/api/catalog")
                .post(body)
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        // test
        assertThat(response.code()).isEqualTo(200);
        List<Asset> actualAssets = Arrays.asList(MAPPER.readValue(Objects.requireNonNull(response.body()).string(), Asset[].class));
        compareAssetsById(actualAssets, assets);
    }

    private Asset buildAsset() {
        return Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .name("demo-test")
                .build();
    }

    private void compareAssetsById(List<Asset> actual, List<Asset> expected) {
        List<String> actualAssetIds = actual.stream().map(Asset::getId).sorted().collect(Collectors.toList());
        List<String> expectedAssetIds = expected.stream().map(Asset::getId).sorted().collect(Collectors.toList());
        assertThat(actualAssetIds).isEqualTo(expectedAssetIds);
    }
}
