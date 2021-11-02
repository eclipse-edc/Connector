package org.eclipse.dataspaceconnector.transfer.httproxy;

import org.eclipse.dataspaceconnector.spi.transfer.synchronous.DataProxy;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;


public class RestDataProxy implements DataProxy {
    private static final String DESTINATION_TYPE_HTTP = "http";
    private final String rootPath;

    public RestDataProxy(String baseUrl) {
        rootPath = baseUrl;
    }

    @Override
    public boolean canHandle(DataRequest request) {
        return request.getDestinationType().equals(DESTINATION_TYPE_HTTP);
    }

    @Override
    public Object getData(DataRequest request) {
        return null;
    }
}
