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

package org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * Stores {@link ContractNegotiation}s and their associated types such as {@link ContractAgreement}s.
 * <p>
 */
@ExtensionPoint
public interface ContractNegotiationStore extends StateEntityStore<ContractNegotiation> {

    /**
     * Returns the contract agreement for the contract id or null.
     */
    @Nullable
    ContractAgreement findContractAgreement(String contractId);

    /**
     * Returns the contract agreement for the contract id, scoped to the given participant context. An agreement that
     * exists but belongs to a different participant context is not returned.
     *
     * @param participantContextId the id of the participant context that owns the agreement.
     * @param contractId           the contract agreement id.
     * @return the {@link ContractAgreement} if found within the participant context, null otherwise.
     */
    @Nullable
    default ContractAgreement findContractAgreement(String participantContextId, String contractId) {
        var query = queryByParticipantContextId(participantContextId)
                .filter(criterion("id", "=", contractId))
                .build();
        try (var agreements = queryAgreements(query)) {
            return agreements.findFirst().orElse(null);
        }
    }

    /**
     * Removes a contract negotiation for the given id.
     */
    StoreResult<Void> deleteById(String negotiationId);

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
     * Finds all contract agreement that are covered by a specific {@link QuerySpec}. If no
     * {@link QuerySpec#getSortField()} is specified, results are not explicitly sorted.
     *
     * @param querySpec The query spec, e.g. paging, filtering, etc.
     * @return a stream of ContractAgreement, cannot be null.
     */
    @NotNull
    Stream<ContractAgreement> queryAgreements(QuerySpec querySpec);

}
