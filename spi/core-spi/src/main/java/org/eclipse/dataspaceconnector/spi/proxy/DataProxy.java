package org.eclipse.dataspaceconnector.spi.proxy;

import org.eclipse.dataspaceconnector.spi.result.Result;

public interface DataProxy {
    Result<ProxyEntry> getData(DataProxyRequest request);
}
