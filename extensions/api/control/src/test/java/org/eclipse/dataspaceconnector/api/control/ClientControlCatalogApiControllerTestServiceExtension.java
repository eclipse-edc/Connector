package org.eclipse.dataspaceconnector.api.control;

import org.assertj.core.util.Sets;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.util.HashMap;
import java.util.Set;

class ClientControlCatalogApiControllerTestServiceExtension implements ServiceExtension {
    private static final String NAME = "EDC Control API Test extension";

    private Monitor monitor;
    private AssetLoader assetLoader;
    private ContractDefinitionStore contractDefinitionStore;

    @Override
    public Set<String> requires() {
        return Sets.newLinkedHashSet(AssetLoader.FEATURE, "edc:core:contract");
    }

    @Override
    public Set<String> provides() {
        return Sets.newHashSet();
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        assetLoader = context.getService(AssetLoader.class);
        contractDefinitionStore = context.getService(ContractDefinitionStore.class);
    }

    @Override
    public void start() {
        Asset asset = Asset.Builder.newInstance()
                .properties(new HashMap<String, Object>() {
                    {
                        put("ids:fileName", "filename1");
                        put("ids:byteSize", 1234);
                        put("ids:fileExtension", "txt");
                    }
                })
                .id("1").build();
        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .type("AzureStorage")
                .property("account", "edc101")
                .property("container", "provider")
                .property("blobname", "data1.txt")
                .keyName("provider-blob-storage-key")
                .build();
        assetLoader.accept(asset, dataAddress);

        Asset asset2 = Asset.Builder.newInstance()
                .properties(new HashMap<String, Object>() {
                    {
                        put("ids:fileName", "filename2");
                        put("ids:byteSize", 5678);
                        put("ids:fileExtension", "pdf");
                    }
                })
                .id("2").build();
        DataAddress dataAddress2 = DataAddress.Builder.newInstance()
                .type("AzureStorage")
                .property("account", "edc101")
                .property("container", "provider")
                .property("blobname", "data2.txt")
                .keyName("provider-blob-storage-key")
                .build();
        assetLoader.accept(asset2, dataAddress2);

        Policy publicPolicy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .target("1")
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .build())
                .build();

        Policy publicPolicy2 = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .target("2")
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .build())
                .build();

        ContractDefinition contractDefinition1 = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(publicPolicy)
                .contractPolicy(publicPolicy)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals("asset:prop:id", "1").build())
                .build();

        ContractDefinition contractDefinition2 = ContractDefinition.Builder.newInstance()
                .id("2")
                .accessPolicy(publicPolicy2)
                .contractPolicy(publicPolicy2)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals("asset:prop:id", "2").build())
                .build();

        contractDefinitionStore.save(contractDefinition1);
        contractDefinitionStore.save(contractDefinition2);

        monitor.info(String.format("Started %s", NAME));
    }
}
