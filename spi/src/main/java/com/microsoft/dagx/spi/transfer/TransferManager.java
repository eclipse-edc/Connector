package com.microsoft.dagx.spi.transfer;

import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;

/**
 * Manages a request to transfer data.
 */
public interface TransferManager {

    /**
     * Returns true if the manager can handle the data type.
     */
    boolean canHandle(DataRequest dataRequest);

    /**
     * Initiate the transfer.
     */
    TransferResponse initiateTransfer(DataRequest request);

}
