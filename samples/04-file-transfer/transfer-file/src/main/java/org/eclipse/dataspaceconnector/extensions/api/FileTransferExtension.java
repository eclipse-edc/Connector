package org.eclipse.dataspaceconnector.extensions.api;

import org.eclipse.dataspaceconnector.metadata.memory.InMemoryAssetIndex;
import org.eclipse.dataspaceconnector.metadata.memory.InMemoryDataAddressResolver;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.util.Set;

import static org.eclipse.dataspaceconnector.policy.model.Operator.IN;
import static org.eclipse.dataspaceconnector.spi.types.domain.asset.AssetProperties.POLICY_ID;

public class FileTransferExtension implements ServiceExtension {

    public static final String USE_EU_POLICY = "use-eu";

    @Override
    public Set<String> requires() {
        return Set.of("edc:webservice", PolicyRegistry.FEATURE, DataAddressResolver.FEATURE, AssetIndex.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var dataFlowMgr = context.getService(DataFlowManager.class);
        var dataAddressResolver = context.getService(DataAddressResolver.class);

        var flowController = new FileTransferFlowController(context.getMonitor(), dataAddressResolver);
        dataFlowMgr.register(flowController);


        registerDataEntries(context);
        savePolicies(context);
        context.getMonitor().info("File Transfer Extension initialized!");
    }

    private void savePolicies(ServiceExtensionContext context) {
        PolicyRegistry policyRegistry = context.getService(PolicyRegistry.class);

        LiteralExpression spatialExpression = new LiteralExpression("ids:absoluteSpatialPosition");
        var euConstraint = AtomicConstraint.Builder.newInstance().leftExpression(spatialExpression).operator(IN).rightExpression(new LiteralExpression("eu")).build();
        var euUsePermission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("idsc:USE").build()).constraint(euConstraint).build();
        var euPolicy = Policy.Builder.newInstance().id(USE_EU_POLICY).permission(euUsePermission).build();
        policyRegistry.registerPolicy(euPolicy);
    }

    private void registerDataEntries(ServiceExtensionContext context) {
        InMemoryAssetIndex assetIndex = (InMemoryAssetIndex) context.getService(AssetIndex.class);
        InMemoryDataAddressResolver dataAddressResolver = (InMemoryDataAddressResolver) context.getService(DataAddressResolver.class);

        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .property("type", "File")
                .property("path", "/home/paul/Documents/")
                .property("filename", "test-document.txt")
                .build();

        String assetId = "test-document";
        Asset asset = Asset.Builder.newInstance().id(assetId).property(POLICY_ID, USE_EU_POLICY).build();

        assetIndex.add(asset, dataAddress);
        dataAddressResolver.add(assetId, dataAddress);
    }
}
