/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.transfer;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

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
