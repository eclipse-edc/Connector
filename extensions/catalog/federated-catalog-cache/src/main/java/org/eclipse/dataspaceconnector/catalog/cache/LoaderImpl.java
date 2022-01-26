package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.catalog.spi.Loader;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;

import java.util.Collection;

public class LoaderImpl implements Loader {
    private final FederatedCacheStore store;

    public LoaderImpl(FederatedCacheStore store) {
        this.store = store;
    }

    @Override
    public void load(Collection<UpdateResponse> responses) {

        for (var response : responses) {
            var catalog = response.getCatalog();
            catalog.getContractOffers().forEach(store::save);
        }
    }
}
