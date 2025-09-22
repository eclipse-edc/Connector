/*
 *  Copyright (c) 2020 - 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.controlplane.transfer.spi;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.entity.StateEntityManager;
import org.eclipse.edc.spi.response.StatusResult;

/**
 * Manages data transfer processes. Currently synchronous and asynchronous data transfers are supported.
 */
@ExtensionPoint
public interface TransferProcessManager extends StateEntityManager {

    /**
     * Initiates a data transfer process on the consumer.
     */
    StatusResult<TransferProcess> initiateConsumerRequest(ParticipantContext participantContext, TransferRequest transferRequest);

}
