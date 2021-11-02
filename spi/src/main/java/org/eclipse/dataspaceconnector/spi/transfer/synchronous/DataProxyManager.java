package org.eclipse.dataspaceconnector.spi.transfer.synchronous;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

public interface DataProxyManager {
    String FEATURE = "edc:transfer:dataproxyregistry";

    void addProxy(DataProxy proxy);

    DataProxy getProxy(DataRequest dataRequest);
}
