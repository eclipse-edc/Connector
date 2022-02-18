package com.siemens.mindsphere.datalake.edc.http;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataReader;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

public class DataLakeReader implements DataReader {
    public DataLakeReader(DataLakeClient dataLakeClient, Monitor monitor) {
        this.dataLakeClient = dataLakeClient;
        this.monitor = monitor;
    }

    private DataLakeClient dataLakeClient;

    private Monitor monitor;

    @Override
    public boolean canHandle(String type) {
        return DataLakeSchema.TYPE.equals(type);
    }

    @Override
    public Result<ByteArrayInputStream> read(DataAddress source) {
        // get target path
        final String targetPath = source.getProperty("path");
        // get pre-signed URL
        final URL targetUrl = dataLakeClient.getDownloadUrl(targetPath);
        // read the URL
        try {
            return Result.success(new ByteArrayInputStream(targetUrl.openStream().readAllBytes()));
        } catch (IOException e) {
            return Result.failure("Error while getting the resource input stream");
        }
    }
}
