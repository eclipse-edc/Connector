package com.microsoft.dagx.transfer.nifi;

import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;

@FunctionalInterface
public interface NifiTransferEndpointConverter {
    NifiTransferEndpoint convert(DataAddress dataAddress);
}
