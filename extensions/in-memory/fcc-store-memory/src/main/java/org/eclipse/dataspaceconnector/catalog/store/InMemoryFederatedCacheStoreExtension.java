package org.eclipse.dataspaceconnector.catalog.store;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.spi.asset.CriterionConverter;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Set;
import java.util.function.Predicate;

public class InMemoryFederatedCacheStoreExtension implements ServiceExtension {
    @Override
    public Set<String> provides() {
        return Set.of(FederatedCacheStore.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        //todo: converts every criterion into a predicate that is always true. must be changed later!
        CriterionConverter<Predicate<Asset>> predicateCriterionConverter = criterion -> asset -> true;
        context.registerService(FederatedCacheStore.class, new InMemoryFederatedCacheStore(predicateCriterionConverter));
        context.getMonitor().info("Initialized In-Memory Federated Cache Store");
    }
}
