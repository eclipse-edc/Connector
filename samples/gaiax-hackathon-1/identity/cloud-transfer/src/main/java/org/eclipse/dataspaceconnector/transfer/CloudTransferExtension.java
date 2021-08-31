package org.eclipse.dataspaceconnector.transfer;

import org.eclipse.dataspaceconnector.policy.model.*;
import org.eclipse.dataspaceconnector.spi.metadata.MetadataStore;
import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.GenericDataCatalogEntry;

import java.util.List;

import static org.eclipse.dataspaceconnector.policy.model.Operator.IN;

public class CloudTransferExtension implements ServiceExtension {
    public static final String USE_EU_POLICY = "use-eu";
    public static final String USE_US_OR_EU_POLICY = "use-us-eu";

    @Override
    public void initialize(ServiceExtensionContext context) {
        var dataFlowMgr = context.getService(DataFlowManager.class);
        var flowController = new BlobToS3DataFlowController(context.getService(Vault.class), context.getMonitor(), context.getTypeManager());
        dataFlowMgr.register(flowController);


        registerDataEntries(context);
        savePolicies(context);
        context.getMonitor().info("Initialized transfer extension");
    }

    private void registerDataEntries(ServiceExtensionContext context) {
        var metadataStore = context.getService(MetadataStore.class);

        GenericDataCatalogEntry file1 = GenericDataCatalogEntry.Builder.newInstance()
                .property("type", "AzureStorage")
                .property("account", "gxhackpaulgpstorage")
                .property("container", "hackathon-src-container")
                .property("blobname", "azure.png")
                .build();

        GenericDataCatalogEntry file3 = GenericDataCatalogEntry.Builder.newInstance()
                .property("type", "AzureStorage")
                .property("account", "gxhackpaulgpstorage")
                .property("container", "hackathon-src-container")
                .property("blobname", "doc1.txt")
                .build();

        GenericDataCatalogEntry file2 = GenericDataCatalogEntry.Builder.newInstance()
                .property("type", "AzureStorage")
                .property("account", "gxhackpaulgpstorage")
                .property("container", "hackathon-src-container")
                .property("blobname", "pinup.webp")
                .build();

        GenericDataCatalogEntry file4 = GenericDataCatalogEntry.Builder.newInstance()
                .property("type", "AzureStorage")
                .property("account", "gxhackpaulgpstorage")
                .property("container", "hackathon-src-container")
                .property("blobname", "index.jpg")
                .build();

        DataEntry entry1 = DataEntry.Builder.newInstance().id("azure.png").policyId(USE_US_OR_EU_POLICY).catalogEntry(file1).build();
        DataEntry entry2 = DataEntry.Builder.newInstance().id("pinup.webp").policyId(USE_US_OR_EU_POLICY).catalogEntry(file2).build();
        DataEntry entry3 = DataEntry.Builder.newInstance().id("doc1.txt").policyId(USE_US_OR_EU_POLICY).catalogEntry(file3).build();
        DataEntry entry4 = DataEntry.Builder.newInstance().id("index.jpg").policyId(USE_US_OR_EU_POLICY).catalogEntry(file4).build();
        metadataStore.save(entry1);
        metadataStore.save(entry2);
        metadataStore.save(entry3);
        metadataStore.save(entry4);
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
