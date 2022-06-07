/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.dataspaceconnector.spi.contract.negotiation.store;

import org.eclipse.dataspaceconnector.spi.persistence.StateEntityStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Stores {@link ContractNegotiation}s and their associated types such as {@link ContractAgreement}s.
 * <p>
 */
public interface ContractNegotiationStore extends StateEntityStore<ContractNegotiation> {

    /**
     * Finds the contract negotiation for the id or null.
     */
    @Nullable
    ContractNegotiation find(String negotiationId);

    /**
     * Returns the contract negotiation for the correlation id provided by the consumer or null.
     */
    @Nullable
    ContractNegotiation findForCorrelationId(String correlationId);

    /**
     * Returns the contract agreement for the contract id or null.
     */
    @Nullable
    ContractAgreement findContractAgreement(String contractId);

    /**
     * Persists a contract negotiation. This follows UPSERT semantics, so if the object didn't exit before, it's
     * created.
     */
    void save(ContractNegotiation negotiation);

    /**
     * Removes a contract negotiation for the given id.
     */
    void delete(String negotiationId);

    /**
     * Finds all contract negotiations that are covered by a specific {@link QuerySpec}. If no
     * {@link QuerySpec#getSortField()} is specified, results are not explicitly sorted.
     * <p>
     * The general order of precedence of the query parameters is:
     * <pre>
     * filter &gt; sort &gt; limit
     * </pre>
     * <p>
     *
     * @param querySpec The query spec, e.g. paging, filtering, etc.
     * @return a stream of ContractNegotiation, cannot be null.
     */
    @NotNull
    Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec);

    /**
     * Finds all contract agreements that are based on a specific contract definition
     *
     * @param definitionId the contract definition id
     * @return a stream of ContractAgreement, cannot be null.
     */
    @NotNull
    Stream<ContractAgreement> getAgreementsForDefinitionId(String definitionId);


    /**
     * Finds all contract agreement that are covered by a specific {@link QuerySpec}. If no
     * {@link QuerySpec#getSortField()} is specified, results are not explicitly sorted.
     *
     * @param querySpec The query spec, e.g. paging, filtering, etc.
     * @return a stream of ContractAgreement, cannot be null.
     */
    @NotNull
    Stream<ContractAgreement> queryAgreements(QuerySpec querySpec);

    /**
     * Finds all negotiations, that have agreements targeting the given asset
     *
     * @param assetId The asset for which the negotiations + agreements are wanted.
     * @return A stream of contract negotiations, or empty
     */
    Stream<ContractNegotiation> getNegotiationsWithAgreementOnAsset(String assetId);
}
