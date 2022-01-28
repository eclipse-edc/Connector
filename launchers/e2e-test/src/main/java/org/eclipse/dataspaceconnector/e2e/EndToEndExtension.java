package org.eclipse.dataspaceconnector.e2e;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

@Requires({ AssetLoader.class, ContractDefinitionStore.class })
public class EndToEndExtension implements ServiceExtension {

    @Override
    public String name() {
        return "End to End";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var assetLoader = context.getService(AssetLoader.class);
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("File").build();
        assetLoader.accept(asset, dataAddress);

        var contractDefinitionStore = context.getService(ContractDefinitionStore.class);
        Policy policy = Policy.Builder.newInstance().build();
        ContractDefinition contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(policy)
                .contractPolicy(policy)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "assetId").build())
                .build();
        contractDefinitionStore.save(contractDefinition);
    }
}
