package org.eclipse.dataspaceconnector.aws.s3.operator;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataReader;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.io.ByteArrayInputStream;

public class S3BucketReader implements DataReader {
    @Override
    public boolean canHandle(String type) {
        return false;
    }

    @Override
    public Result<ByteArrayInputStream> read(DataAddress source) {
        return null;
    }
}
