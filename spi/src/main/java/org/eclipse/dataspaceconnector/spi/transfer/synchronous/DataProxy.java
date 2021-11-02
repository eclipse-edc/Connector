package org.eclipse.dataspaceconnector.spi.transfer.synchronous;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

public interface DataProxy {

    Object getData(DataRequest request);
}
