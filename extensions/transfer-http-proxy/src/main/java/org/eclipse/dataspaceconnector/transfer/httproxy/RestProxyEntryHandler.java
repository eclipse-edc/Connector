package org.eclipse.dataspaceconnector.transfer.httproxy;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntry;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntryHandler;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class RestProxyEntryHandler implements ProxyEntryHandler {
    private final Monitor monitor;
    private final OkHttpClient httpClient;

    public RestProxyEntryHandler(Monitor monitor, OkHttpClient httpClient) {
        this.monitor = monitor;
        this.httpClient = httpClient;
    }

    @Override
    public Object accept(DataRequest originalRequest, ProxyEntry restProxyEntry) {
        monitor.info("RestProxyEntryHandler accepted entry");
        var url = restProxyEntry.getProperties().get("url").toString();
        var token = restProxyEntry.getProperties().get("token");
        monitor.info(String.format("Will issue a GET request to %s ", url));

        var segments = url.split("/");

        var proxyUrlBuilder = HttpUrl.parse(originalRequest.getConnectorAddress())
                .newBuilder()
                .addPathSegment("api");

        Arrays.stream(segments).forEach(proxyUrlBuilder::addPathSegment);

        var request = new Request.Builder().url(proxyUrlBuilder.build())
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (var response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return Objects.requireNonNull(response.body()).string();
            }

            monitor.warning("Could not access REST Proxy: " + response);
            return restProxyEntry;
        } catch (IOException e) {
            monitor.warning("Error accessing REST Proxy " + e.getMessage());
            throw new EdcException(e);
        }
    }

}
