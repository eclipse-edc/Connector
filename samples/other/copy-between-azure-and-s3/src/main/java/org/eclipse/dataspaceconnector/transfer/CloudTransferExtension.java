package org.eclipse.dataspaceconnector.transfer;

import org.eclipse.dataspaceconnector.metadata.memory.InMemoryAssetIndex;
import org.eclipse.dataspaceconnector.metadata.memory.InMemoryDataAddressResolver;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.OrConstraint;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.util.List;

import static java.util.Arrays.asList;
import static org.eclipse.dataspaceconnector.policy.model.Operator.IN;
import static org.eclipse.dataspaceconnector.spi.types.domain.asset.AssetProperties.POLICY_ID;

public class CloudTransferExtension implements ServiceExtension {
    public static final String USE_EU_POLICY = "use-eu";
    public static final String USE_US_OR_EU_POLICY = "use-us-eu";

    @Override
    public void initialize(ServiceExtensionContext context) {
        var dataFlowMgr = context.getService(DataFlowManager.class);
        DataAddressResolver dataAddressResolver = context.getService(DataAddressResolver.class);
        var flowController = new BlobToS3DataFlowController(context.getService(Vault.class), context.getMonitor(), context.getTypeManager(), dataAddressResolver);
        dataFlowMgr.register(flowController);


        registerDataEntries(context);
        savePolicies(context);
        context.getMonitor().info("Initialized transfer extension");
    }

    private void registerDataEntries(ServiceExtensionContext context) {
        InMemoryAssetIndex assetIndex = (InMemoryAssetIndex) context.getService(AssetIndex.class);
        InMemoryDataAddressResolver dataAddressResolver = (InMemoryDataAddressResolver) context.getService(DataAddressResolver.class);

        asList("azure.png", "doc1.txt", "pinup.webp", "index.jpg")
                .forEach(filename -> {
                    DataAddress dataAddress = DataAddress.Builder.newInstance()
                            .property("type", "AzureStorage")
                            .property("account", "gxhackpaulgpstorage")
                            .property("container", "hackathon-src-container")
                            .property("blobname", "azure.png")
                            .build();

                    Asset asset = Asset.Builder.newInstance().id(filename).property(POLICY_ID, USE_US_OR_EU_POLICY).build();

                    assetIndex.add(asset, dataAddress);
                    dataAddressResolver.add(filename, dataAddress);
                });
    }

    private void savePolicies(ServiceExtensionContext context) {
        PolicyRegistry policyRegistry = context.getService(PolicyRegistry.class);

        LiteralExpression spatialExpression = new LiteralExpression("ids:absoluteSpatialPosition");
        var euConstraint = AtomicConstraint.Builder.newInstance().leftExpression(spatialExpression).operator(IN).rightExpression(new LiteralExpression("eu")).build();
        var euUsePermission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("idsc:USE").build()).constraint(euConstraint).build();
        var euPolicy = Policy.Builder.newInstance().id(USE_EU_POLICY).permission(euUsePermission).build();
        policyRegistry.registerPolicy(euPolicy);

        var usConstraint = AtomicConstraint.Builder.newInstance().leftExpression(spatialExpression).operator(IN).rightExpression(new LiteralExpression("us")).build();
        var usOrEuConstrain = OrConstraint.Builder.newInstance().constraints(List.of(euConstraint, usConstraint)).build();
        var usOrEuPermission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("idsc:USE").build()).constraint(usOrEuConstrain).build();
        var usOrEuPolicy = Policy.Builder.newInstance().id(USE_US_OR_EU_POLICY).permission(usOrEuPermission).build();
        policyRegistry.registerPolicy(usOrEuPolicy);
    }

}
