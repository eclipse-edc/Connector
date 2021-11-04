package org.eclipse.dataspaceconnector.spi.transfer.synchronous;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.spi.types.domain.Polymorphic;

@JsonTypeName("dataspaceconnector:proxyentry")
public class ProxyEntry implements Polymorphic {

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

    @Override
    public String toString() {
        return "ProxyEntry{" +
                "url='" + url + '\'' +
                '}';
    }
}
