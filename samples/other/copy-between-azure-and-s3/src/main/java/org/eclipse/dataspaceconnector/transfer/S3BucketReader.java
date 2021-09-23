package org.eclipse.dataspaceconnector.transfer;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.io.ByteArrayInputStream;

class S3BucketReader implements DataReader {
    @Override
    public ByteArrayInputStream read(DataAddress source) {
        throw new UnsupportedOperationException("this operation is not yet implemented!");
    }
}
