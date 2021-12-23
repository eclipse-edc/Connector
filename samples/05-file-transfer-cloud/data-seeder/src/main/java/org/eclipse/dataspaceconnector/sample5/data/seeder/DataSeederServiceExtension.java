package org.eclipse.dataspaceconnector.sample5.data.seeder;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Requires({ DataAddressResolver.class, AssetLoader.class })
public class DataSeederServiceExtension implements ServiceExtension {

    private FakeSetup fakeSetup;

    @Override
    public String name() {
        return "Sample 5 Data Seeder";
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        var monitor = serviceExtensionContext.getMonitor();

        AssetLoader assetLoader = serviceExtensionContext.getService(AssetLoader.class);
        ContractDefinitionStore contractDefinitionStore = serviceExtensionContext.getService(ContractDefinitionStore.class);
        fakeSetup = new FakeSetup(monitor, assetLoader, contractDefinitionStore);
    }

    @Override
    public void start() {
        fakeSetup.setupAssets();
        fakeSetup.setupContractOffers();
    }

}
