package org.eclipse.dataspaceconnector.extensions.transfer;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

@FunctionalInterface
public interface DataReader {
    byte[] read(DataAddress source);
}
