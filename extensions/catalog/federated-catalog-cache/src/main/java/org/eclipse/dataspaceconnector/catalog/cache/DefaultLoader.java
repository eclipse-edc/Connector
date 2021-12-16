package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.catalog.spi.Loader;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;

import java.util.Collection;

public class DefaultLoader implements Loader {
    private final FederatedCacheStore store;

    public DefaultLoader(FederatedCacheStore store) {
        this.store = store;
    }

    @Override
    public void load(Collection<UpdateResponse> responses) {

        for (var response : responses) {
            var catalog = response.getCatalog();
            var originator = response.getSource();

            catalog.getContractOffers().forEach(contractOffer -> {
//                var asset = CachedAsset.Builder.newInstance()
//                        .copyFrom(receivedAsset)
//                        .originator(originator)
//                        //.policy(somePolicy) //not yet implemented
//                        .build();
                store.save(contractOffer);
            });

        }
    }
}
