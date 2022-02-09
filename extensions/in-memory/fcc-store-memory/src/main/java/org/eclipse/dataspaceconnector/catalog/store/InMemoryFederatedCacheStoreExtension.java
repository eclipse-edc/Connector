package org.eclipse.dataspaceconnector.catalog.store;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.spi.query.CriterionConverter;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import java.util.function.Predicate;

@Provides(FederatedCacheStore.class)
public class InMemoryFederatedCacheStoreExtension implements ServiceExtension {

    @Override
    public String name() {
        return "In-Memory Federated Cache Store";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        //todo: converts every criterion into a predicate that is always true. must be changed later!
        CriterionConverter<Predicate<ContractOffer>> predicateCriterionConverter = criterion -> offer -> true;
        context.registerService(FederatedCacheStore.class, new InMemoryFederatedCacheStore(predicateCriterionConverter));
    }
}
