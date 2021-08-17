package org.eclipse.dataspaceconnector.transfer;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

@FunctionalInterface
public interface DataWriter {
    public void write(DataAddress destination, byte[] data);
}
