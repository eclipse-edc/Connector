/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.spi.contract.negotiation.store;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Stores {@link ContractNegotiation}s and their associated types such as {@link ContractAgreement}s.
 *
 * TODO: This is work-in-progress
 */
public interface ContractNegotiationStore {

    /**
     * Finds the contract negotiation for the id or null.
     */
    @Nullable
    ContractNegotiation find(String negotiationId);

    /**
     * Returns the contract negotiation for the correlation id provided by the client or null.
     */
    @Nullable
    ContractNegotiation findForCorrelationId(String correlationId);

    /**
     * Returns the contract agreement for the id or null.
     */
    @Nullable
    ContractAgreement findContractAgreement(String contractId);

    /**
     * Persists a contract negotiation.
     */
    void create(ContractNegotiation negotiation);

    /**
     * Updates a contract negotiation.
     */
    void update(ContractNegotiation negotiation);

    /**
     * Removes a contract negotiation for the given id.
     */
    void delete(String negotiationId);

    /**
     * Returns the next batch of contract negotiations for the given state.
     */
    @NotNull
    List<ContractNegotiation> nextForState(int state, int max);

}
