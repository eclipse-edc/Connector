package org.eclipse.dataspaceconnector.sample5.data.seeder;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class DataSeederServiceExtension implements ServiceExtension {

    private static final String NAME = "Sample 5 Data Seeder extension";

    private Monitor monitor;
    private FakeSetup fakeSetup;

    @Override
    public Set<String> requires() {
        return Set.of(DataAddressResolver.FEATURE, AssetLoader.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        monitor = serviceExtensionContext.getMonitor();


        AssetLoader assetLoader = serviceExtensionContext.getService(AssetLoader.class);
        ContractDefinitionStore contractDefinitionStore = serviceExtensionContext.getService(ContractDefinitionStore.class);
        fakeSetup = new FakeSetup(monitor, assetLoader, contractDefinitionStore);

        monitor.info(String.format("Initialized %s", NAME));
    }

    @Override
    public void start() {
        monitor.info(String.format("Started %s", NAME));
        fakeSetup.setupAssets();
        fakeSetup.setupContractOffers();
    }

    @Override
    public void shutdown() {
        monitor.info(String.format("Shutdown %s", NAME));
    }

}
