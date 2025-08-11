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

package org.eclipse.edc.connector.controlplane.services.spi.transferprocess;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.NotNull;

/**
 * Mediates access to and modification of {@link TransferProcess}es on protocol messages reception.
 */
public interface TransferProcessProtocolService {

    /**
     * Notifies the TransferProcess that it has been requested by the counter-part.
     *
     * @param message             the incoming message
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    @NotNull
    ServiceResult<TransferProcess> notifyRequested(TransferRequestMessage message, TokenRepresentation tokenRepresentation);

    /**
     * Notifies the TransferProcess that it has been started by the counter-part.
     *
     * @param message             the incoming message
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    @NotNull
    ServiceResult<TransferProcess> notifyStarted(TransferStartMessage message, TokenRepresentation tokenRepresentation);

    /**
     * Notifies the TransferProcess that it has been completed by the counter-part.
     *
     * @param message             the incoming message
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    @NotNull
    ServiceResult<TransferProcess> notifyCompleted(TransferCompletionMessage message, TokenRepresentation tokenRepresentation);

    /**
     * Notifies the TransferProcess that it has been suspended by the counter-part.
     *
     * @param message             the incoming message
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    @NotNull
    ServiceResult<TransferProcess> notifySuspended(TransferSuspensionMessage message, TokenRepresentation tokenRepresentation);

    /**
     * Notifies the TransferProcess that it has been terminated by the counter-part.
     *
     * @param message             the incoming message
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    @NotNull
    ServiceResult<TransferProcess> notifyTerminated(TransferTerminationMessage message, TokenRepresentation tokenRepresentation);

    /**
     * Finds a transfer process that has been requested by the counter-part. An existing
     * process, for which the counter-part is not authorized, is treated as non-existent.
     *
     * @param id                  id of the transfer process
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result containing the transfer process if it was found, a failed one otherwise
     */
    @NotNull
    ServiceResult<TransferProcess> findById(String id, TokenRepresentation tokenRepresentation, String protocol);
}
