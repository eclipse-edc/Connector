/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.store.cosmos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;

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
@JsonTypeName("dagx:transferprocessdocument")
public class TransferProcessDocument {

    @JsonUnwrapped
    private TransferProcess wrappedInstance;

    @JsonProperty
    private String partitionKey;

    @JsonProperty
    private Lease lease;

    protected TransferProcessDocument() {
        //Jackson does not yet support the combination of @JsonUnwrapped and a @JsonProperty annotation in a constructor
    }

    private TransferProcessDocument(TransferProcess wrappedInstance, String partitionKey) {
        this.wrappedInstance = wrappedInstance;
        this.partitionKey = partitionKey;
    }

    public static TransferProcessDocument from(TransferProcess process, String partitionKey) {
        return new TransferProcessDocument(process, partitionKey);
    }


    public String getPartitionKey() {
        return partitionKey;
    }

    public TransferProcess getWrappedInstance() {
        return wrappedInstance;
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
            var endDate = Instant.ofEpochMilli(lease.getLeasedUntil());
            throw new IllegalStateException("This document is leased by " + lease.getLeasedBy() + "on " + startDate.toString() + " and cannot be leased again until " + endDate.toString() + "!");
        }
    }

}
