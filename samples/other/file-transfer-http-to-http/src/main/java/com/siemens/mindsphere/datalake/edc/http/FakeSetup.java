package com.siemens.mindsphere.datalake.edc.http;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

public class FakeSetup {
    public FakeSetup(Monitor monitor, AssetLoader assetIndexLoader, ContractDefinitionStore contractDefinitionStore, String url) {
        this.monitor = monitor;
        this.assetIndexLoader = assetIndexLoader;
        this.contractDefinitionStore = contractDefinitionStore;
        this.url = url;
    }

    private final Monitor monitor;
    private final AssetLoader assetIndexLoader;
    private final ContractDefinitionStore contractDefinitionStore;
    private final String url;

    public void setupAssets() {
        Asset asset = Asset.Builder.newInstance().id("1").build();
        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .type(HttpSchema.TYPE)
                .property(HttpSchema.URL, url)
                .keyName("demo.jpg")
                .build();
        assetIndexLoader.accept(asset, dataAddress);

        monitor.info("Register http sample Asset: 1");
    }

    public void setupContractOffers() {
        Policy publicPolicy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .target("1")
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .build())
                .build();

        ContractDefinition contractDefinition1 = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(publicPolicy)
                .contractPolicy(publicPolicy)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "1").build())
                .build();

        contractDefinitionStore.save(contractDefinition1);
    }
}
