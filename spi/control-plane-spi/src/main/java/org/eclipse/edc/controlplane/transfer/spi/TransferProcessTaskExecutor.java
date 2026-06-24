/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.controlplane.transfer.spi;

import org.eclipse.edc.controlplane.transfer.spi.tasks.TransferProcessTaskPayload;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.response.StatusResult;

/**
 * A service interface for handling transfer process tasks.
 * Implementations can be registered to process various transfer process tasks.
 */
@ExtensionPoint
public interface TransferProcessTaskExecutor {

    /**
     * Handles a transfer process task.
     *
     * @param task the transfer process task
     * @return a StatusResult indicating success or failure
     */
    StatusResult<Void> handle(TransferProcessTaskPayload task);

}
