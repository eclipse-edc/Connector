package com.microsoft.dagx.transfer.nifi;

import com.microsoft.dagx.spi.DagxException;

public class NifiTransferException extends DagxException {
    public NifiTransferException(String message) {
        super(message);
    }
}
