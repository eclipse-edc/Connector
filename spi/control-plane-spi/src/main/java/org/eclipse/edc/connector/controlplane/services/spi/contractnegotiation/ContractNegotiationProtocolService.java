/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Schaeffler AG - GetDspRequest refactor
 *
 */

package org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.NotNull;

/**
 * Mediates access to and modification of {@link ContractNegotiation}s on protocol messages reception.
 */
public interface ContractNegotiationProtocolService {

    /**
     * Notifies the ContractNegotiation that it has been requested by the consumer.
     * Only callable on provider ContractNegotiation.
     *
     * @param message             the incoming message
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    @NotNull
    ServiceResult<ContractNegotiation> notifyRequested(ParticipantContext participantContext, ContractRequestMessage message, TokenRepresentation tokenRepresentation);

    /**
     * Notifies the ContractNegotiation that it has been offered by the provider.
     * Only callable on consumer ContractNegotiation.
     *
     * @param message             the incoming message
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    @NotNull
    ServiceResult<ContractNegotiation> notifyOffered(ParticipantContext participantContext, ContractOfferMessage message, TokenRepresentation tokenRepresentation);

    /**
     * Notifies the ContractNegotiation that it has been agreed by the accepted.
     * Only callable on provider ContractNegotiation.
     *
     * @param message             the incoming message
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    @NotNull
    ServiceResult<ContractNegotiation> notifyAccepted(ParticipantContext participantContext, ContractNegotiationEventMessage message, TokenRepresentation tokenRepresentation);

    /**
     * Notifies the ContractNegotiation that it has been agreed by the provider.
     * Only callable on consumer ContractNegotiation.
     *
     * @param message             the incoming message
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    @NotNull
    ServiceResult<ContractNegotiation> notifyAgreed(ParticipantContext participantContext, ContractAgreementMessage message, TokenRepresentation tokenRepresentation);

    /**
     * Notifies the ContractNegotiation that it has been verified by the consumer.
     * Only callable on provider ContractNegotiation.
     *
     * @param message             the incoming message
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    @NotNull
    ServiceResult<ContractNegotiation> notifyVerified(ParticipantContext participantContext, ContractAgreementVerificationMessage message, TokenRepresentation tokenRepresentation);

    /**
     * Notifies the ContractNegotiation that it has been finalized by the provider.
     * Only callable on consumer ContractNegotiation.
     *
     * @param message             the incoming message
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    @NotNull
    ServiceResult<ContractNegotiation> notifyFinalized(ParticipantContext participantContext, ContractNegotiationEventMessage message, TokenRepresentation tokenRepresentation);

    /**
     * Notifies the ContractNegotiation that it has been terminated by the counter-part.
     *
     * @param message             the incoming message
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    @NotNull
    ServiceResult<ContractNegotiation> notifyTerminated(ParticipantContext participantContext, ContractNegotiationTerminationMessage message, TokenRepresentation tokenRepresentation);

    /**
     * Finds a contract negotiation that has been requested by the counter-part. An existing
     * negotiation, for which the counter-part is not authorized, is treated as non-existent.
     *
     * @param message                  the incoming message
     * @param tokenRepresentation the counter-party claim token
     * @return a succeeded result containing the negotiation if it was found, a failed one otherwise
     */
    @NotNull
    ServiceResult<ContractNegotiation> findById(ParticipantContext participantContext, ContractNegotiationRequestMessage message, TokenRepresentation tokenRepresentation);
}
