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

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Stores {@link ContractNegotiation}s and their associated types such as {@link ContractAgreement}s.
 * <p>
 */
@Feature(ContractNegotiationStore.FEATURE)
public interface ContractNegotiationStore {

    String FEATURE = "edc:core:contract:contractnegotiation:store";

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
     * Returns the contract agreement for the id or null.
     */
    @Nullable
    ContractAgreement findContractAgreement(String contractId);

    /**
     * Persists a contract negotiation. This follows UPSERT semantics, so if the object didn't exit before, it's created.
     */
    void save(ContractNegotiation negotiation);

    /**
     * Removes a contract negotiation for the given id.
     */
    void delete(String negotiationId);

    /**
     * Returns the next batch of contract negotiations for the given state.
     */
    @NotNull
    List<ContractNegotiation> nextForState(int state, int max);

    /**
     * Finds all contract negotiations that are covered by a specific {@link QuerySpec}. If no {@link QuerySpec#getSortField()}
     * is specified, results are not explicitly sorted.
     * <p>
     * The general order of precedence of the query parameters is:
     * <pre>
     * filter > sort > limit
     * </pre>
     * <p>
     *
     * @param querySpec The query spec, e.g. paging, filtering, etc.
     * @return A potentially empty stream of {@link ContractNegotiation}, never null.
     */
    Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec);

}
