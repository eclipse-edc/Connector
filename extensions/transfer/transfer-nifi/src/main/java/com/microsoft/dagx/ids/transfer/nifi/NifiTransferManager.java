package com.microsoft.dagx.ids.transfer.nifi;

import com.microsoft.dagx.spi.transfer.TransferManager;
import com.microsoft.dagx.spi.transfer.TransferResponse;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;

import java.net.URI;

import static com.microsoft.dagx.spi.transfer.TransferResponse.Status.OK;

public class NifiTransferManager implements TransferManager {

    @Override
    public boolean canHandle(URI dataUrn) {
        // handle everything for now
        return true;
    }

    @Override
    public TransferResponse initiateTransfer(DataRequest request) {
        // do nothing for now
        return new TransferResponse(OK);
    }
}
