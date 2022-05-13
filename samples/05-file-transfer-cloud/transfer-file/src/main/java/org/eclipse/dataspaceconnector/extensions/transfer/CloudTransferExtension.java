/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.extensions.transfer;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.aws.s3.core.S3ClientProviderImpl;
import org.eclipse.dataspaceconnector.aws.s3.operator.S3BucketReader;
import org.eclipse.dataspaceconnector.aws.s3.operator.S3BucketWriter;
import org.eclipse.dataspaceconnector.azure.blob.core.api.BlobStoreApi;
import org.eclipse.dataspaceconnector.azure.blob.operator.BlobStoreReader;
import org.eclipse.dataspaceconnector.azure.blob.operator.BlobStoreWriter;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.transfer.core.inline.InlineDataFlowController;

import java.time.temporal.ChronoUnit;

public class CloudTransferExtension implements ServiceExtension {
    public static final String USE_EU_POLICY = "use-eu";
    @Inject
    private DataFlowManager dataFlowMgr;
    @Inject
    private DataAddressResolver dataAddressResolver;
    @Inject
    private BlobStoreApi blobStoreApi;
    @Inject
    private DataOperatorRegistry dataOperatorRegistry;
    @Inject
    private Vault vault;

    @Override
    public String name() {
        return "Cloud-Based Transfer";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerFlowController(context);
        registerDataEntries(context);
    }

    private void registerFlowController(ServiceExtensionContext context) {
        var s3ClientProvider = new S3ClientProviderImpl();

        dataOperatorRegistry.registerReader(new BlobStoreReader(blobStoreApi));
        dataOperatorRegistry.registerReader(new S3BucketReader(context.getMonitor(), vault, s3ClientProvider));


        RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                .withBackoff(500, 5000, ChronoUnit.MILLIS)
                .withMaxRetries(3);

        dataOperatorRegistry.registerWriter(new S3BucketWriter(context.getMonitor(), context.getTypeManager(), retryPolicy, s3ClientProvider));
        dataOperatorRegistry.registerWriter(new BlobStoreWriter(context.getMonitor(), context.getTypeManager()));

        dataFlowMgr.register(new InlineDataFlowController(vault, context.getMonitor(), dataOperatorRegistry));
    }

    private void registerDataEntries(ServiceExtensionContext context) {
        AssetLoader assetIndex = context.getService(AssetLoader.class);

        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .property("type", "AzureStorage")
                .property("container", "src-container")
                .property("blobname", "test-document.txt")
                .build();

        String assetId = "test-document";
        Asset asset = Asset.Builder.newInstance().id(assetId).build();

        assetIndex.accept(asset, dataAddress);
    }
}
