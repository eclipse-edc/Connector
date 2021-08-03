/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.edc.spi.transfer;

import org.eclipse.edc.spi.types.domain.transfer.TransferProcess;

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
