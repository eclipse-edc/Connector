package com.microsoft.dagx.spi.transfer;

import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;

import java.net.URI;

/**
 * Manages a request to transfer data.
 */
public interface TransferManager {

    /**
     * Returns true if the manager can handle the data type.
     *
     * @param dataUrn the URN of the requested data
     */
    boolean canHandle(URI dataUrn);

    /**
     * Initiate the transfer.
     */
    TransferResponse initiateTransfer(DataRequest request);

}
