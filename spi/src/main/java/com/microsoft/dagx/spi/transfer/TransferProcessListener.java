/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.spi.transfer;

import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;

public interface TransferProcessListener {
    default void completed(TransferProcess process) {
    }

    default void deprovisioned(TransferProcess process) {
    }
}
