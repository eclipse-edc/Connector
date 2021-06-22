/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import java.util.Arrays;

/**
 * Defines the states a client and provider transfer process can be in.
 */
public enum TransferProcessStates {
    UNSAVED(0),
    INITIAL(100),
    PROVISIONING(200),
    PROVISIONED(300),
    REQUESTED(400),
    REQUESTED_ACK(500),
    IN_PROGRESS(600),
    STREAMING(700),
    COMPLETED(800),
    DEPROVISIONING(900),
    DEPROVISIONING_REQ(901),
    DEPROVISIONED(1000),
    ENDED(1100),
    ERROR(-1);

    private final int code;

    TransferProcessStates(int code) {
        this.code = code;
    }

    public static TransferProcessStates from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }
}
