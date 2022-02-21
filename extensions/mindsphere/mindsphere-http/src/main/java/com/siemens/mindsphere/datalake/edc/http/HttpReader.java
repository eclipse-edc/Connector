package com.siemens.mindsphere.datalake.edc.http;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataReader;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpReader implements DataReader {
    public HttpReader(Monitor monitor) {
        this.monitor = monitor;
    }

    private Monitor monitor;

    @Override
    public boolean canHandle(String type) {
        return HttpSchema.TYPE.equals(type);
    }

    @Override
    public Result<ByteArrayInputStream> read(DataAddress source) {
        monitor.info("Reading from HTTP asset: " + source.getKeyName());
        final String urlString = source.getProperty("url");
        if (urlString == null) {
            return Result.failure("No source URL provided");
        }

        try {
            final URL url = new URL(urlString);
            try (final InputStream inputStream = url.openStream()) {
                return Result.success(new ByteArrayInputStream(inputStream.readAllBytes()));
            }
        } catch (MalformedURLException e) {
            return Result.failure("Malformed URL provided");
        } catch (IOException e) {
            return Result.failure("Unable to read from source");
        }
    }
}
