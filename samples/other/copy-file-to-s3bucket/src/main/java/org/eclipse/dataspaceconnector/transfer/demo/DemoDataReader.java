package org.eclipse.dataspaceconnector.transfer.demo;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.transfer.inline.spi.DataReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class DemoDataReader implements DataReader {
    @Override
    public boolean canHandle(String type) {
        return true;
    }

    @Override
    public Result<ByteArrayInputStream> read(DataAddress source) {
        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("demo_image.jpg");
        try {
            Objects.requireNonNull(resourceAsStream);
            return Result.success(new ByteArrayInputStream(resourceAsStream.readAllBytes()));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}
