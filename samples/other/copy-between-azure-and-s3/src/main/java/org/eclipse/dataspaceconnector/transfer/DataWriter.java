package org.eclipse.dataspaceconnector.transfer;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.io.InputStream;

@FunctionalInterface
public interface DataWriter {
    void write(DataAddress destination, String name, InputStream data, String secretToken);
}
