package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.CatalogQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.CatalogQueryAdapterRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class InMemoryProtocolAdapterRegistry implements CatalogQueryAdapterRegistry {

    private final Map<String, List<CatalogQueryAdapter>> map;

    InMemoryProtocolAdapterRegistry() {
        this.map = new ConcurrentHashMap<>();
    }


    @Override
    public Collection<CatalogQueryAdapter> findForProtocol(String protocolName) {
        if (!map.containsKey(protocolName)) return Collections.emptyList();
        return map.get(protocolName);
    }

    @Override
    public void register(String protocolName, CatalogQueryAdapter adapter) {
        if (!map.containsKey(protocolName)) {
            map.put(protocolName, new ArrayList<>());
        }
        map.get(protocolName).add(adapter);
    }

    @Override
    public void unregister(String protocolName, CatalogQueryAdapter adapter) {
        if (map.containsKey(protocolName)) {
            map.get(protocolName).remove(adapter);

            if (map.get(protocolName).isEmpty()) {
                map.remove(protocolName);
            }
        }
    }
}
