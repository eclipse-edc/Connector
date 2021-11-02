package org.eclipse.dataspaceconnector.transfer.core.synchronous;

import org.eclipse.dataspaceconnector.spi.transfer.synchronous.DataProxy;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.DataProxyManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

import java.util.ArrayList;
import java.util.List;

public class DataProxyManagerImpl implements DataProxyManager {
    private final List<DataProxy> proxies;

    public DataProxyManagerImpl() {
        proxies = new ArrayList<>();
    }

    @Override
    public void addProxy(DataProxy proxy) {
        if (!proxies.contains(proxy)) {
            proxies.add(proxy);
        }
    }

    @Override
    public DataProxy getProxy(DataRequest dataRequest) {
        return proxies.stream().filter(p -> p.canHandle(dataRequest)).findFirst().orElse(null);
    }
}
