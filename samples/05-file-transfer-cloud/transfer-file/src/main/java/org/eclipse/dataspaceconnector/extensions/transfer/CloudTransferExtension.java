package org.eclipse.dataspaceconnector.extensions.transfer;

import net.jodah.failsafe.RetryPolicy;
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
        var vault = context.getService(Vault.class);
        dataOperatorRegistry.registerReader(new BlobStoreReader(blobStoreApi));
        dataOperatorRegistry.registerReader(new S3BucketReader());


        RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                .withBackoff(500, 5000, ChronoUnit.MILLIS)
                .withMaxRetries(3);
        dataOperatorRegistry.registerWriter(new S3BucketWriter(context.getMonitor(), context.getTypeManager(), retryPolicy));
        dataOperatorRegistry.registerWriter(new BlobStoreWriter(context.getMonitor(), context.getTypeManager()));

        dataFlowMgr.register(new InlineDataFlowController(vault, context.getMonitor(), dataOperatorRegistry, dataAddressResolver));
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
