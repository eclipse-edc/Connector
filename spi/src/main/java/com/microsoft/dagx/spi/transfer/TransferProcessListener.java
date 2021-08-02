/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.spi.transfer;

import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;

public interface TransferProcessListener {
    default void created(TransferProcess process) {
    }

    default void provisioning(TransferProcess process) {
    }

    default void provisioned(TransferProcess process) {
    }

    default void inProgress(TransferProcess process) {
    }

    default void completed(TransferProcess process) {
    }

    default void deprovisioning(TransferProcess process) {
    }

    default void deprovisioned(TransferProcess process) {
    }

    default void ended(TransferProcess process) {
    }

    default void error(TransferProcess process) {
    }

    default void requested(TransferProcess process) {
    }
}
