/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.demo.nifi;

import org.eclipse.dataspaceconnector.policy.model.*;
import org.eclipse.dataspaceconnector.schema.azure.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.spi.metadata.MetadataStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.GenericDataCatalogEntry;

import java.util.List;

import static org.eclipse.dataspaceconnector.policy.model.Operator.IN;

/**
 * Loads data for the Nifi-based demo.
 */
public class NifiDemoServiceExtension implements ServiceExtension {
    public static final String USE_EU_POLICY = "use-eu";
    public static final String USE_US_OR_EU_POLICY = "use-us-eu";
    private Monitor monitor;
    private ServiceExtensionContext context;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        this.context = context;

        monitor.info("Initialized Nifi Demo extension");
    }

    @Override
    public void start() {
        saveDataEntries();
        savePolicies();
        monitor.info("Started Nifi Demo extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Nifi Demo extension");
    }

    private void saveDataEntries() {
        MetadataStore metadataStore = context.getService(MetadataStore.class);

        GenericDataCatalogEntry sourceFileCatalog = GenericDataCatalogEntry.Builder.newInstance()
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, "edcdemogpstorage")
                .property(AzureBlobStoreSchema.CONTAINER_NAME, "src-container")
                .property(AzureBlobStoreSchema.BLOB_NAME, "IMG_1971.jpg")
                .property("keyName", "lili.jpg")
                .property("type", AzureBlobStoreSchema.TYPE)
                .build();

        DataEntry entry1 = DataEntry.Builder.newInstance().id("test123").policyId(USE_EU_POLICY).catalogEntry(sourceFileCatalog).build();
        metadataStore.save(entry1);

        DataEntry entry2 = DataEntry.Builder.newInstance().id("test456").policyId(USE_US_OR_EU_POLICY).catalogEntry(sourceFileCatalog).build();
        metadataStore.save(entry2);
    }

    private void savePolicies() {
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
