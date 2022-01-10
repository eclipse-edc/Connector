package org.eclipse.dataspaceconnector.spi.transfer.synchronous;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

public interface DataProxy {

    ProxyEntry getData(DataRequest request);
}
