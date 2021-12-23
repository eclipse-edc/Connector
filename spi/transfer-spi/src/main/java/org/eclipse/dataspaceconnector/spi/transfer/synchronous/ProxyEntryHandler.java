package org.eclipse.dataspaceconnector.spi.transfer.synchronous;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

/**
 * Handler to process {@link ProxyEntry} objects on the client side of a synchronous data transfer. For example if the ProxyEntry contains
 * an HTTP url and a token, the handler could be a simple REST client that performs the request.
 */
@FunctionalInterface
public interface ProxyEntryHandler {
    Object accept(DataRequest originalRequest, ProxyEntry proxyEntry);
}
