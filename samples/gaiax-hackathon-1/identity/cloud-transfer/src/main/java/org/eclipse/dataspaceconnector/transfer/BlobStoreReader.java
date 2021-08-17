package org.eclipse.dataspaceconnector.transfer;

import org.eclipse.dataspaceconnector.common.azure.BlobStoreApi;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

class BlobStoreReader implements DataReader {
    private final BlobStoreApi blobStoreApi;

    BlobStoreReader(BlobStoreApi blobStoreApi) {
        this.blobStoreApi = blobStoreApi;
    }

    @Override
    public byte[] read(DataAddress source) {
        var account = source.getProperty("account");
        var container = source.getProperty("container");
        var blobName = source.getProperty("blobname");
        return blobStoreApi.getBlob(account, container, blobName);
    }
}
