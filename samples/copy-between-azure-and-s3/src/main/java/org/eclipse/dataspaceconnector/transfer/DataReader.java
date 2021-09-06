package org.eclipse.dataspaceconnector.transfer;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

@FunctionalInterface
public interface DataReader {
    byte[] read(DataAddress source);
}
