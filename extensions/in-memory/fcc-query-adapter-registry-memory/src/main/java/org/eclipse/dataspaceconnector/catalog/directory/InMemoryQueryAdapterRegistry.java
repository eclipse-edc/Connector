package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.QueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.QueryAdapterRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class InMemoryQueryAdapterRegistry implements QueryAdapterRegistry {

    private final Queue<QueryAdapter> registry = new ConcurrentLinkedQueue<>();

    @Override
    public Collection<QueryAdapter> getAllAdapters() {
        return new ArrayList<>(registry);
    }

    @Override
    public void register(QueryAdapter adapter) {
        registry.add(adapter);
    }
}
