/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.spi.types.protocol;

import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;
import org.jetbrains.annotations.NotNull;

/**
 * A remote message related to the TransferProcess context
 */
public interface TransferRemoteMessage extends ProcessRemoteMessage {

    /**
     * Returns the process id.
     *
     * @return the processId property.
     */
    @Override
    @NotNull
    String getProcessId();

    /**
     * Returns the {@link Policy} associated with the Transfer Process.
     *
     * @return the transfer process {@link Policy}.
     */
    Policy getPolicy();
}
