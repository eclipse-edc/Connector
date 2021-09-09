/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.nifi;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.policy.model.*;
import org.eclipse.dataspaceconnector.schema.SchemaRegistry;
import org.eclipse.dataspaceconnector.schema.azure.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.metadata.MetadataStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.GenericDataCatalogEntry;

import java.util.List;
import java.util.Set;

import static org.eclipse.dataspaceconnector.policy.model.Operator.IN;

public class NifiTransferExtension implements ServiceExtension {
    public static final String USE_EU_POLICY = "use-eu";
    public static final String USE_US_OR_EU_POLICY = "use-us-eu";
    @EdcSetting
    private static final String URL_SETTING = "edc.nifi.url";
    @EdcSetting
    private static final String URL_SETTING_FLOW = "edc.nifi.flow.url";
    private static final String DEFAULT_NIFI_URL = "http://localhost:8080";
    private static final String DEFAULT_NIFI_FLOW_URL = "http://localhost:8888";
    private static final String PROVIDES_NIFI = "nifi";
    private Monitor monitor;
    private ServiceExtensionContext context;

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.context = context;
        monitor = context.getMonitor();

        registerConverters(context);
        registerManager(context);

        monitor.info("Initialized Core Transfer extension");
    }

    private void registerConverters(ServiceExtensionContext context) {
        Vault vault = context.getService(Vault.class);
        var sr = context.getService(SchemaRegistry.class);

        var converter = new NifiTransferEndpointConverter(sr, vault, context.getTypeManager());
        context.registerService(NifiTransferEndpointConverter.class, converter);
    }

    @Override
    public Set<String> provides() {
        return Set.of(PROVIDES_NIFI);
    }

    @Override
    public Set<String> requires() {
        return Set.of(SchemaRegistry.FEATURE, "dataspaceconnector:http-client");
    }

    @Override
    public void start() {
        saveDataEntries();
        savePolicies();
        monitor.info("Started Transfer Demo extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Nifi Transfer extension");
    }

    private void registerManager(ServiceExtensionContext context) {
        DataFlowManager dataFlowManager = context.getService(DataFlowManager.class);

        String url = context.getSetting(URL_SETTING, DEFAULT_NIFI_URL);
        String flowUrl = context.getSetting(URL_SETTING_FLOW, DEFAULT_NIFI_FLOW_URL);

        var httpClient = context.getService(OkHttpClient.class);

        NifiTransferManagerConfiguration configuration = NifiTransferManagerConfiguration.Builder.newInstance().url(url).flowUrl(flowUrl).build();

        var converter = context.getService(NifiTransferEndpointConverter.class);

        NifiDataFlowController manager = new NifiDataFlowController(configuration, context.getTypeManager(), context.getMonitor(), context.getService(Vault.class), httpClient, converter);
        dataFlowManager.register(manager);
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
