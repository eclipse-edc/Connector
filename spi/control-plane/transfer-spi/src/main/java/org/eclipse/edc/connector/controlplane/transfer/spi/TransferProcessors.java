/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.concurrent.CompletableFuture;

/**
 * Logic for the transfer manager
 */
public interface TransferProcessors {

    /**
     * Process INITIAL transfer.
     * Invoke preparation phase.
     *
     * @param process the transfer process.
     * @return success or failure.
     */
    CompletableFuture<StatusResult<Void>> processConsumerInitial(TransferProcess process);

    /**
     * Process INITIAL transfer.
     * Invoke startup phase.
     *
     * @param process the transfer process.
     * @return success or failure.
     */
    CompletableFuture<StatusResult<Void>> processProviderInitial(TransferProcess process);

    /**
     * Process REQUESTING transfer<p> If CONSUMER, send request to the provider, should never be PROVIDER
     *
     * @param process the REQUESTING transfer fetched
     * @return if the transfer has been processed or not
     */
    CompletableFuture<StatusResult<Void>> processRequesting(TransferProcess process);

    /**
     * Process STARTUP_REQUESTED transfer for consumer<p> Notify data-plane that data flow has been started
     *
     * @param process the STARTUP_REQUESTED transfer fetched
     * @return if the transfer has been processed or not
     */
    CompletableFuture<StatusResult<Void>> processStartupRequested(TransferProcess process);

    /**
     * Process STARTING transfer<p> If PROVIDER, starts data transfer and send message to consumer, should never be CONSUMER
     *
     * @param process the STARTING transfer fetched
     * @return if the transfer has been processed or not
     */
    CompletableFuture<StatusResult<Void>> processStarting(TransferProcess process);

    /**
     * Process RESUMING transfer for PROVIDER.
     *
     * @param process the RESUMING transfer fetched
     * @return if the transfer has been processed or not
     */
    CompletableFuture<StatusResult<Void>> processProviderResuming(TransferProcess process);

    /**
     * Process STARTING transfer that was SUSPENDED
     *
     * @param process the STARTING transfer fetched
     * @return if the transfer has been processed or not
     */
    CompletableFuture<StatusResult<Void>> processConsumerResuming(TransferProcess process);

    /**
     * Process COMPLETING transfer<p> Send COMPLETED message to counter-part
     *
     * @param process the COMPLETING transfer fetched
     * @return if the transfer has been processed or not
     */
    CompletableFuture<StatusResult<Void>> processCompleting(TransferProcess process);

    /**
     * Process SUSPENDING transfer<p>
     * Suspend data flow unless it's CONSUMER and send SUSPENDED message to counter-part.
     *
     * @param process the SUSPENDING transfer fetched
     * @return if the transfer has been processed or not
     */
    CompletableFuture<StatusResult<Void>> processSuspending(TransferProcess process);

    /**
     * Process TERMINATING transfer<p>
     * Stop data flow unless it's CONSUMER and send TERMINATED message to counter-part.
     *
     * @param process the TERMINATING transfer fetched
     * @return if the transfer has been processed or not
     */
    CompletableFuture<StatusResult<Void>> processTerminating(TransferProcess process);

}
