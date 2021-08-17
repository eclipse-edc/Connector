package org.eclipse.dataspaceconnector.transfer;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

class BlobStoreReader implements DataReader {
    @Override
    public byte[] read(DataAddress source) {
        return new byte[0];
    }
}
