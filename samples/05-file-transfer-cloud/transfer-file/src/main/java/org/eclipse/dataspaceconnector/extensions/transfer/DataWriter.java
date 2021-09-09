package org.eclipse.dataspaceconnector.extensions.transfer;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

@FunctionalInterface
public interface DataWriter {
    public void write(DataAddress destination, String name, byte[] data, String secretToken);
}
