package org.eclipse.dataspaceconnector.azure.blob.operator;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureSasToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataWriter;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

public class BlobStoreWriter implements DataWriter {
    private final TypeManager typeManager;
    private final Monitor monitor;

    public BlobStoreWriter(Monitor monitor, TypeManager typeManager) {
        this.typeManager = typeManager;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(String type) {
        return AzureBlobStoreSchema.TYPE.equals(type);
    }

    @Override
    public Result<Void> write(DataAddress destination, String name, InputStream data, String secretToken) {
        var accountName = destination.getProperty("account");
        var container = destination.getProperty("container");

        AzureSasToken sasToken = null;
        try {
            sasToken = typeManager.readValue(secretToken, AzureSasToken.class);
        } catch (Exception e) {
            var message = "Cannot interpret temporary secret as valid AzureSasToken!";
            monitor.severe(message);
            return Result.failure(message);
        }


        Objects.requireNonNull(accountName, "accountName");
        if (secretToken == null) {
            throw new IllegalArgumentException("BlobStoreWriter secretToken cannot be null!");
        } else {
            var endpoint = "https://" + accountName + ".blob.core.windows.net";

            var sas = sasToken.getSas();
            if (sas.startsWith("?")) {
                sas = sas.substring(1);
            }

            uploadBlob(name, data, container, endpoint, sas);
            uploadBlob(name + ".complete", new ByteArrayInputStream(new byte[0]), container, endpoint, sas);
            return Result.success();
        }
    }

    private void uploadBlob(String name, InputStream data, String container, String endpoint, String sas) {
        BlobClient blobClient = new BlobClientBuilder()
                .endpoint(endpoint)
                .sasToken(sas)
                .containerName(container)
                .blobName(name)
                .buildClient();

        blobClient.upload(BinaryData.fromStream(data), true);
    }
}
