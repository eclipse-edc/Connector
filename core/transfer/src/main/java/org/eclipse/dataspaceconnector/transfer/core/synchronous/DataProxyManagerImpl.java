package org.eclipse.dataspaceconnector.transfer.core.synchronous;

import org.eclipse.dataspaceconnector.spi.proxy.DataProxy;
import org.eclipse.dataspaceconnector.spi.proxy.DataProxyManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataProxyManagerImpl implements DataProxyManager {
    private final Map<String, DataProxy> proxies;

    public DataProxyManagerImpl() {
        proxies = new ConcurrentHashMap<>();
    }

    @Override
    public void addProxy(String type, DataProxy proxy) {
        proxies.put(type, proxy);
    }

    @Override
    public DataProxy getProxy(String type) {
        return proxies.get(type);
    }
}
