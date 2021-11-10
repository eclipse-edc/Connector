package org.eclipse.dataspaceconnector.spi.transfer.synchronous;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.spi.types.domain.Polymorphic;

@JsonTypeName("dataspaceconnector:proxyentry")
public class ProxyEntry implements Polymorphic {

    private final String url;
    private final String token;
    private final String type;

    @JsonCreator
    public ProxyEntry(@JsonProperty("url") String rootPath, @JsonProperty("token") String token, String type) {
        url = rootPath;
        this.token = token;
        this.type = type;
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

    public String getType() {
        return type;
    }
}
