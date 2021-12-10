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

package org.eclipse.dataspaceconnector.transfer.store.cosmos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDocument;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.time.Instant;

/**
 * This is a wrapper solely used to store {@link TransferProcess} objects in an Azure CosmosDB. Some features or requirements
 * of CosmosDB don't fit into a {@link TransferProcess}'s data model, such as a "partition key" or a "lease".
 * The former is required by CosmosDB to achieve a better distribution of read/write load and the latter was implemented
 * to guard against multiple connectors processing the same TransferProcess.
 *
 * @see Lease
 * @see TransferProcess
 */
@JsonTypeName("dataspaceconnector:transferprocessdocument")
public class TransferProcessDocument extends CosmosDocument<TransferProcess> {

    @JsonProperty
    private Lease lease;

    @JsonCreator
    public TransferProcessDocument(@JsonProperty("wrappedInstance") TransferProcess wrappedInstance, @JsonProperty("partitionKey") String partitionKey) {
        super(wrappedInstance, partitionKey);
    }

    @Override
    public String getId() {
        return getWrappedInstance().getId();
    }

    public Lease getLease() {
        return lease;
    }

    /**
     * Tries to lock down the TransferProcess to avoid concurrent modification. No database modification takes place.
     *
     * @param connectorId The ID of the connector that attempts acquiring the lease.
     * @throws IllegalStateException if the {@link TransferProcessDocument} has been leased before by a different connector
     */
    public void acquireLease(String connectorId) {
        if (lease == null || lease.getLeasedBy().equals(connectorId)) {
            lease = new Lease(connectorId);
        } else {
            var startDate = Instant.ofEpochMilli(lease.getLeasedAt());
            var endDate = startDate.plusSeconds(lease.getLeaseDuration());
            throw new IllegalStateException("This document is leased by " + lease.getLeasedBy() + "on " + startDate + " and cannot be leased again until " + endDate.toString() + "!");
        }
    }
}
