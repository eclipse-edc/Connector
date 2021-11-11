package org.eclipse.dataspaceconnector.spi.transfer.synchronous;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

@FunctionalInterface
public interface ProxyEntryHandler {
    Object accept(DataRequest originalRequest, ProxyEntry proxyEntry);
}
