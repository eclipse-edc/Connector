package org.eclipse.dataspaceconnector.transfer.httproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProxyEntry {

    private final String url;
    private final String token;

    @JsonCreator
    public ProxyEntry(@JsonProperty("url") String rootPath, @JsonProperty("token") String token) {
        url = rootPath;
        this.token = token;
    }

    public String getUrl() {
        return url;
    }

    public String getToken() {
        return token;
    }
}
