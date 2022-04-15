package com.siemens.mindsphere.datalake.edc.http;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.transfer.core.inline.InlineDataFlowController;

public class TransferExtension implements ServiceExtension {
    @EdcSetting
    private static final String STUB_URL = "edc.demo.http.source.url";

    @Inject
    private DataFlowManager dataFlowMgr;
    @Inject
    private DataAddressResolver dataAddressResolver;
    @Inject
    private DataOperatorRegistry dataOperatorRegistry;
    @Inject
    private ContractDefinitionLoader contractDefinitionLoader;
    @Inject
    private PolicyStore policyStore;

    private FakeSetup fakeSetup;

    @Override
    public String name() {
        return "HTTP to HTTP Transfer";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);
        var monitor = context.getMonitor();
        var assetLoader = context.getService(AssetLoader.class);

        dataOperatorRegistry.registerWriter(new HttpWriter(monitor));
        dataOperatorRegistry.registerReader(new HttpReader(monitor));

        dataFlowMgr.register(new InlineDataFlowController(vault, context.getMonitor(), dataOperatorRegistry));

        final String assetUrl = context.getSetting(STUB_URL, "missing");

        fakeSetup = new FakeSetup(monitor, assetLoader, contractDefinitionLoader, policyStore, assetUrl);
    }

    @Override
    public void start() {
        fakeSetup.setupAssets();
        fakeSetup.setupContractOffers();
    }
}
