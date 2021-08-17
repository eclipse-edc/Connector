package org.eclipse.dataspaceconnector.transfer;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResponse;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public class BlobToS3DataFlowController implements DataFlowController {
    private static final List<String> ALLOWED_TYPES = Arrays.asList("AmazonS3", "AzureStorage");
    private final Vault vault;
    private final Monitor monitor;

    public BlobToS3DataFlowController(Vault vault, Monitor monitor) {

        this.vault = vault;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {

        String sourceType = dataRequest.getDataEntry().getCatalogEntry().getAddress().getType();
        String destinationType = dataRequest.getDestinationType();

        return verifyType(sourceType) && verifyType(destinationType);
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiateFlow(DataRequest dataRequest) {
        String sourceType = dataRequest.getDataEntry().getCatalogEntry().getAddress().getType();
        String destinationType = dataRequest.getDestinationType();

        var destSecretName = dataRequest.getDataDestination().getKeyName();
        if (destSecretName == null) {
            monitor.severe(format("No credentials found for %s, will not copy!", destinationType));
            return new DataFlowInitiateResponse(ResponseStatus.ERROR_RETRY, "Did not find credentials for data destination.");
        }
        var secret = vault.resolveSecret(destSecretName);

        monitor.info(format("Copying data from %s to %s", sourceType, destinationType));

        var reader = getReader(sourceType);
        var writer = getWriter(destinationType);

        var data = reader.read(dataRequest.getDataEntry().getCatalogEntry().getAddress());

        writer.write(dataRequest.getDataDestination(), data);

        monitor.info("Copying done");
        return DataFlowInitiateResponse.OK;
    }

    private @NotNull DataWriter getWriter(String destinationType) {
        switch (destinationType) {
            case "AmazonS3":
                return new S3BucketWriter();
            case "AzureStorage":
                return new BlobStoreWriter();
        }
        throw new IllegalArgumentException("Unknown source type " + destinationType);
    }

    private @NotNull DataReader getReader(String sourceType) {
        switch (sourceType) {
            case "AmazonS3":
                return new S3BucketReader();
            case "AzureStorage":
                return new BlobStoreReader();
        }
        throw new IllegalArgumentException("Unknown source type " + sourceType);
    }

    private boolean verifyType(String type) {
        return ALLOWED_TYPES.contains(type);
    }
}
