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
 *
 */

package org.eclipse.edc.connector.spi.contractnegotiation;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementRequest;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.stream.Stream;

public interface ContractNegotiationService {

    /**
     * Returns a contract negotiation by its id
     *
     * @param contractNegotiationId id of the contract negotiation
     * @return the contract negotiation, null if it's not found
     */
    ContractNegotiation findbyId(String contractNegotiationId);

    /**
     * Query contract negotiations
     *
     * @param query request
     * @return the collection of contract negotiations that match the query
     */
    ServiceResult<Stream<ContractNegotiation>> query(QuerySpec query);

    /**
     * Get negotiation state
     *
     * @param negotiationId the id of contract negotiation
     * @return the state of the contract negotiation, null if it does not exist
     */
    String getState(String negotiationId);

    /**
     * Fetches the {@linkplain ContractAgreement} for a given {@linkplain ContractNegotiation}, or null if either the
     * negotiation does not exist, or no agreement has yet been reached.
     *
     * @param negotiationId the id of contract negotiation
     * @return the contract agreement, null if the negotiation does not exist or no agreement attached.
     */
    ContractAgreement getForNegotiation(String negotiationId);

    /**
     * Initiate contract negotiation
     *
     * @param request the contract offer request
     * @return the contract negotiation initiated
     */
    ContractNegotiation initiateNegotiation(ContractOfferRequest request);

    /**
     * Cancel a contract negotiation
     *
     * @param negotiationId the id of the contract negotiation to be canceled
     * @return successful result if the contract negotiation is canceled correctly, failure otherwise
     */
    ServiceResult<ContractNegotiation> cancel(String negotiationId);

    /**
     * Decline a contract negotiation
     *
     * @param negotiationId the id of the contract negotiation to be declined
     * @return successful result if the contract negotiation is declined correctly, failure otherwise
     */
    ServiceResult<ContractNegotiation> decline(String negotiationId);

    /**
     * Notifies the ContractNegotiation that it has been requested by the consumer.
     * Only callable on provider ContractNegotiation.
     *
     * @param message the incoming message
     * @param claimToken the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    ServiceResult<ContractNegotiation> notifyConsumerRequested(ContractOfferRequest message, ClaimToken claimToken);

    /**
     * Notifies the ContractNegotiation that it has been offered by the provider.
     * Only callable on consumer ContractNegotiation.
     *
     * @param message the incoming message
     * @param claimToken the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    ServiceResult<ContractNegotiation> notifyProviderOffered(ContractOfferRequest message, ClaimToken claimToken);

    /**
     * Notifies the ContractNegotiation that it has been agreed by the consumer.
     * Only callable on provider ContractNegotiation.
     *
     * @param message the incoming message
     * @param claimToken the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    ServiceResult<ContractNegotiation> notifyConsumerAgreed(ContractNegotiationEventMessage message, ClaimToken claimToken);

    /**
     * Notifies the ContractNegotiation that it has been agreed by the provider.
     * Only callable on consumer ContractNegotiation.
     *
     * @param message the incoming message
     * @param claimToken the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    ServiceResult<ContractNegotiation> notifyProviderAgreed(ContractAgreementRequest message, ClaimToken claimToken);

    /**
     * Notifies the ContractNegotiation that it has been verified by the consumer.
     * Only callable on provider ContractNegotiation.
     *
     * @param message the incoming message
     * @param claimToken the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    ServiceResult<ContractNegotiation> notifyConsumerVerified(ContractAgreementVerificationMessage message, ClaimToken claimToken);

    /**
     * Notifies the ContractNegotiation that it has been finalized by the provider.
     * Only callable on consumer ContractNegotiation.
     *
     * @param message the incoming message
     * @param claimToken the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    ServiceResult<ContractNegotiation> notifyProviderFinalized(ContractNegotiationEventMessage message, ClaimToken claimToken);

    /**
     * Notifies the ContractNegotiation that it has been terminated by the counter-part.
     *
     * @param message the incoming message
     * @param claimToken the counter-party claim token
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    ServiceResult<ContractNegotiation> notifyTerminated(TransferTerminationMessage message, ClaimToken claimToken);
}
