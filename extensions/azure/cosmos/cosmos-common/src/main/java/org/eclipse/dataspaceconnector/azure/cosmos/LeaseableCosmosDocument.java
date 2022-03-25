/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.cosmos;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.dataspaceconnector.spi.persistence.Lease;

import java.time.Duration;
import java.time.Instant;

/**
 * Extends the {@link CosmosDocument} by adding a {@link Lease} property. This acts as an exclusive lock: a party leasing a
 * document owns an exclusive lock until the lease expires or it has been explicitly broken.
 * <p>
 * Any attempt to acquire or break the lease on a document that has been leased by someone else results in an {@link IllegalStateException}
 * <p>
 * <strong>Note: acquiring and breaking leases is done in memory, so persisting that change is up to the caller.</strong>
 */
public abstract class LeaseableCosmosDocument<T> extends CosmosDocument<T> {

    @JsonProperty
    protected Lease lease;

    protected LeaseableCosmosDocument(T wrappedInstance, String partitionKey) {
        super(wrappedInstance, partitionKey);
    }

    public Lease getLease() {
        return lease;
    }

    /**
     * Attempts to "break" the lease, effectively setting it to {@code null}.
     *
     * @throws IllegalStateException if the document was leased by another leaser
     */
    public void breakLease(String identifier) {
        if (lease == null) {
            return;
        }
        if (lease.getLeasedBy().equals(identifier)) {
            lease = null;
        } else {
            throw new IllegalStateException("Was not leased by " + identifier);
        }
    }

    /**
     * Tries to lock down the document to avoid concurrent modification. No database modification takes place yet.
     *
     * @param leaseBy The ID of the connector that attempts acquiring the lease.
     * @throws IllegalStateException if the {@link LeaseableCosmosDocument} has been leased before by a different connector
     */
    public void acquireLease(String leaseBy) {
        acquireLease(leaseBy, Duration.ofSeconds(60));
    }

    /**
     * Tries to lock down the TransferProcess to avoid concurrent modification. No database modification takes place yet
     *
     * @param leaseBy       The ID of the connector that attempts acquiring the lease.
     * @param leaseDuration How long the lease should be valid
     * @throws IllegalStateException if the {@link LeaseableCosmosDocument} has been leased before by a different connector
     */
    public void acquireLease(String leaseBy, Duration leaseDuration) {
        if (lease == null || lease.getLeasedBy().equals(leaseBy)) {
            lease = new Lease(leaseBy, Instant.now().toEpochMilli(), leaseDuration.toMillis());
        } else {
            var startDate = Instant.ofEpochMilli(lease.getLeasedAt());
            var endDate = startDate.plusSeconds(lease.getLeaseDuration());
            throw new IllegalStateException(String.format("This document is leased by %s on %s and cannot be leased again until %s!", lease.getLeasedBy(), startDate, endDate.toString()));
        }
    }
}
