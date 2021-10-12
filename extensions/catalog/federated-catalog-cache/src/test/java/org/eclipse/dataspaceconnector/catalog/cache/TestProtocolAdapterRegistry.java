package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapterRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestProtocolAdapterRegistry implements ProtocolAdapterRegistry {
    private final Map<String, List<ProtocolAdapter>> map;

    public TestProtocolAdapterRegistry(ProtocolAdapter... adapters) {
        map = new ConcurrentHashMap<>();
        Arrays.stream(adapters).forEach(a -> register("test-protocol", a));
    }

    @Override
    public Collection<ProtocolAdapter> findForProtocol(String protocolName) {
        if(!map.containsKey(protocolName)) return Collections.emptyList();
        return map.get(protocolName);
    }

    @Override
    public void register(String protocolName, ProtocolAdapter adapter) {
        if (!map.containsKey(protocolName)) {
            map.put(protocolName, new ArrayList<>());
        }
        map.get(protocolName).add(adapter);
    }

    @Override
    public void unregister(String protocolName, ProtocolAdapter adapter) {
        if (map.containsKey(protocolName)) {
            map.get(protocolName).remove(adapter);

            if (map.get(protocolName).isEmpty()) {
                map.remove(protocolName);
            }
        }
    }

    public void clear(){
        map.clear();
    }
}
