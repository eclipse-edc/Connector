package org.eclipse.dataspaceconnector.transfer.httproxy;

import org.eclipse.dataspaceconnector.spi.transfer.synchronous.DataProxy;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

import java.util.UUID;


public class RestDataProxy implements DataProxy {
    public static final String DESTINATION_TYPE_HTTP = "http";
    private final String path;

    public RestDataProxy(String proxyPath) {
        path = proxyPath;
    }

    @Override
    public Object getData(DataRequest request) {
        return new ProxyEntry(path, UUID.randomUUID().toString());
    }
}
