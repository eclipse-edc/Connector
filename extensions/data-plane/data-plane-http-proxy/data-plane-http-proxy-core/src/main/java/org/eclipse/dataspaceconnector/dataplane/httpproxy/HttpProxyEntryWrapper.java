package org.eclipse.dataspaceconnector.dataplane.httpproxy;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.proxy.ProxyEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static org.eclipse.dataspaceconnector.dataplane.httpproxy.HttpProxyConstants.HTTP_PROXY_TYPE;

public class HttpProxyEntryWrapper {

    private static final String URL = "url";
    private static final String TOKEN = "token";
    private static final String EXPIRATION = "exp";

    private final String url;
    private final String token;
    private final long expirationEpochSeconds;

    private HttpProxyEntryWrapper(@NotNull String url, @NotNull String token, long expirationEpochSeconds) {
        this.url = url;
        this.token = token;
        this.expirationEpochSeconds = expirationEpochSeconds;
    }

    public static HttpProxyEntryWrapper from(ProxyEntry entry) {
        return new HttpProxyEntryWrapper(getMandatoryProperty(entry, URL, String.class),
                getMandatoryProperty(entry, TOKEN, String.class),
                getMandatoryProperty(entry, EXPIRATION, Long.class));
    }

    public static ProxyEntry toProxyEntry(@NotNull String url, @NotNull String token, long expirationEpochSeconds) {
        return ProxyEntry.Builder.newInstance()
                .type(HTTP_PROXY_TYPE)
                .properties(Map.of(URL, url, TOKEN, token, EXPIRATION, expirationEpochSeconds))
                .build();
    }

    public String getUrl() {
        return url;
    }

    public String getToken() {
        return token;
    }

    public long getExpirationEpochSeconds() {
        return expirationEpochSeconds;
    }

    private static <T> T getMandatoryProperty(ProxyEntry entry, String key, Class<T> type) {
        Object value = entry.getProperties().get(key);
        if (value == null) {
            throw new EdcException(String.format("Missing key `%s` in ProxyEntry", key));
        }
        return type.cast(value);
    }
}
