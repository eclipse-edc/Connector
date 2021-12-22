/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.contract.negotiation.store.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDocument;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;

import java.time.Duration;
import java.time.Instant;

@JsonTypeName("dataspaceconnector:contractdnegotiationdocument")
public class ContractNegotiationDocument extends CosmosDocument<ContractNegotiation> {
    @JsonProperty
    private Lease lease;

    @JsonCreator
    public ContractNegotiationDocument(@JsonProperty("wrappedInstance") ContractNegotiation contractNegotiation, @JsonProperty("partitionKey") String partitionKey) {
        //todo: lets think about whether this a good partition key
        super(contractNegotiation, partitionKey);
    }

    public Lease getLease() {
        return lease;
    }

    @Override
    public String getId() {
        return getWrappedInstance().getId();
    }

    /**
     * Tries to lock down the TransferProcess to avoid concurrent modification. No database modification takes place.
     *
     * @param connectorId The ID of the connector that attempts acquiring the lease.
     * @throws IllegalStateException if the {@link ContractNegotiationDocument} has been leased before by a different connector
     */
    public void acquireLease(String connectorId) {
        acquireLease(connectorId, Duration.ofSeconds(60));
    }

    /**
     * Tries to lock down the TransferProcess to avoid concurrent modification. No database modification takes place.
     *
     * @param connectorId The ID of the connector that attempts acquiring the lease.
     * @throws IllegalStateException if the {@link ContractNegotiationDocument} has been leased before by a different connector
     */
    public void acquireLease(String connectorId, Duration leaseDuration) {
        if (lease == null || lease.getLeasedBy().equals(connectorId)) {
            lease = new Lease(connectorId, Instant.now().toEpochMilli(), leaseDuration.toMillis());
        } else {
            var startDate = Instant.ofEpochMilli(lease.getLeasedAt());
            var endDate = startDate.plusSeconds(lease.getLeaseDuration());
            throw new IllegalStateException("This document is leased by " + lease.getLeasedBy() + "on " + startDate + " and cannot be leased again until " + endDate.toString() + "!");
        }
    }
}
