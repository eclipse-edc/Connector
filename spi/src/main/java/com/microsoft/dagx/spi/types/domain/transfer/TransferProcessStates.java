package com.microsoft.dagx.spi.types.domain.transfer;

/**
 * Defines the states a transfer process can be in.
 */
public enum TransferProcessStates {
    UNSAVED(0),
    INITIAL(100),
    PROVISIONING(200),
    PROVISIONED(300),
    REQUESTED(400),
    REQUESTED_ACK(500),
    RECEIVED(600),
    STREAMING(700),
    COMPLETED(800),
    DEPROVISIONING(900),
    DEPROVISIONED(1000),
    ENDED(1100),
    ERROR(-1);

    private int code;

    public int code() {
        return code;
    }

    TransferProcessStates(int code) {
        this.code = code;
    }
}
