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

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service;

import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;

import java.util.Collection;

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
    Collection<ContractNegotiation> query(QuerySpec query);

    /**
     * Get negotiation state
     *
     * @param negotiationId the id of contract negotiation
     * @return the state of the contract negotiation, null if it does not exist
     */
    String getState(String negotiationId);

    /**
     * Get negotiation contract agreement if it already exists
     *
     * @param negotiationId the id of contract negotiation
     * @return the contract agreement, null if it does not exists
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
}
