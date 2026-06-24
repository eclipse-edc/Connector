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

package org.eclipse.edc.connector.controlplane.contract.spi.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.concurrent.CompletableFuture;

/**
 * Logic for Negotiation state process
 */
public interface NegotiationProcessors {

    /**
     * Processes {@link ContractNegotiation} in state INITIAL. Transition ContractNegotiation to REQUESTING.
     *
     * @return true if processed, false otherwise
     */
    CompletableFuture<StatusResult<Void>> processInitial(ContractNegotiation negotiation);

    /**
     * Processes {@link ContractNegotiation} in state REQUESTING. Tries to send the current offer to the respective
     * provider. If this succeeds, the ContractNegotiation is transitioned to state REQUESTED. Else, it is transitioned
     * to REQUESTING for a retry.
     *
     * @return true if processed, false otherwise
     */
    CompletableFuture<StatusResult<Void>> processRequesting(ContractNegotiation negotiation);

    /**
     * Processes {@link ContractNegotiation} in state ACCEPTING. If the dispatch succeeds, the
     * ContractNegotiation is transitioned to state ACCEPTED. Else, it is transitioned to ACCEPTING for a retry.
     *
     * @return true if processed, false otherwise
     */
    CompletableFuture<StatusResult<Void>> processAccepting(ContractNegotiation negotiation);

    /**
     * Processes {@link ContractNegotiation} in state AGREED. It transitions to VERIFYING to make the verification process start.
     *
     * @return true if processed, false otherwise
     */
    CompletableFuture<StatusResult<Void>> processAgreed(ContractNegotiation negotiation);

    /**
     * Processes {@link ContractNegotiation} in state VERIFYING. Verifies the Agreement and send the
     * {@link ContractAgreementVerificationMessage} to the provider and transition the negotiation to the VERIFIED
     * state.
     *
     * @return true if processed, false otherwise
     */
    CompletableFuture<StatusResult<Void>> processVerifying(ContractNegotiation negotiation);

    /**
     * Processes {@link ContractNegotiation} in state OFFERING. Tries to send the current offer to the
     * respective consumer. If this succeeds, the ContractNegotiation is transitioned to state OFFERED. Else,
     * it is transitioned to OFFERING for a retry.
     *
     * @return true if processed, false elsewhere
     */
    CompletableFuture<StatusResult<Void>> processOffering(ContractNegotiation negotiation);

    /**
     * Processes {@link ContractNegotiation} in state REQUESTED. It transitions to AGREEING, because the automatic agreement.
     *
     * @return true if processed, false otherwise
     */
    CompletableFuture<StatusResult<Void>> processRequested(ContractNegotiation negotiation);

    /**
     * Processes {@link ContractNegotiation} in state ACCEPTED. It transitions to AGREEING.
     *
     * @return true if processed, false otherwise
     */
    CompletableFuture<StatusResult<Void>> processAccepted(ContractNegotiation negotiation);

    /**
     * Processes {@link ContractNegotiation} in state CONFIRMING. Tries to send a contract agreement to the respective
     * consumer. If this succeeds, the ContractNegotiation is transitioned to state CONFIRMED. Else, it is transitioned
     * to CONFIRMING for a retry.
     *
     * @return true if processed, false elsewhere
     */
    CompletableFuture<StatusResult<Void>> processAgreeing(ContractNegotiation negotiation);

    /**
     * Processes {@link ContractNegotiation} in state VERIFIED. It transitions to FINALIZING to make the finalization process start.
     *
     * @return true if processed, false otherwise
     */
    CompletableFuture<StatusResult<Void>> processVerified(ContractNegotiation negotiation);

    /**
     * Processes {@link ContractNegotiation} in state OFFERING. Tries to send the current offer to the
     * respective consumer. If this succeeds, the ContractNegotiation is transitioned to state OFFERED. Else,
     * it is transitioned to OFFERING for a retry.
     *
     * @return true if processed, false elsewhere
     */
    CompletableFuture<StatusResult<Void>> processFinalizing(ContractNegotiation negotiation);

    /**
     * Processes {@link ContractNegotiation} in state TERMINATING. Tries to send a contract termination to the counter-party.
     * If this succeeds, the ContractNegotiation is transitioned to state TERMINATED. Else, it is transitioned
     * to TERMINATING for a retry.
     *
     * @return success if succeeded, failure otherwise.
     */
    CompletableFuture<StatusResult<Void>> processTerminating(ContractNegotiation negotiation);
}
