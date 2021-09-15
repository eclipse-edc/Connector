package org.eclipse.dataspaceconnector.transfer;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.io.ByteArrayInputStream;

@FunctionalInterface
public interface DataReader {
    ByteArrayInputStream read(DataAddress source);
}
