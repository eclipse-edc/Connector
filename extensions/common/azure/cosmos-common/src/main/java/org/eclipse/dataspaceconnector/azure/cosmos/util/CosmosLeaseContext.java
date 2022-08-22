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

package org.eclipse.dataspaceconnector.azure.cosmos.util;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeExecutor;
import dev.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApi;
import org.eclipse.dataspaceconnector.spi.persistence.LeaseContext;

import java.util.List;
import java.util.Objects;

/**
 * Provides document-agnostic functionality to acquire and break an exclusive lock on the database level by calling a stored
 * procedure.
 *
 * <em>This happens on the database in an ACID way.</em>
 */
public class CosmosLeaseContext implements LeaseContext {
    private static final String LEASE_SPROC_NAME = "lease";
    private final String partitionKey;
    private final CosmosDbApi cosmosDbApi;
    private final String leaseHolder;
    private FailsafeExecutor<Object> retryPolicy;

    private CosmosLeaseContext(CosmosDbApi cosmosDbApi, String partitionKey, String leaseHolder) {
        this.cosmosDbApi = cosmosDbApi;
        this.partitionKey = partitionKey;
        this.leaseHolder = leaseHolder;
    }

    /**
     * Creates a new instance of the {@link CosmosLeaseContext} class
     *
     * @param api          An instance of the {@link CosmosDbApi}
     * @param leaseHolder  A string identifying the holder of the lease, e.g. a connector runtime name
     * @param partitionKey The partition key
     */
    public static CosmosLeaseContext with(CosmosDbApi api, String partitionKey, String leaseHolder) {
        Objects.requireNonNull(api, "CosmosDbApi");
        Objects.requireNonNull(partitionKey, "partitionKey");
        Objects.requireNonNull(leaseHolder, "leaseHolder");
        return new CosmosLeaseContext(api, partitionKey, leaseHolder);
    }

    /**
     * Optionally provide a {@link RetryPolicy} to guard agains transient errors.
     */
    public CosmosLeaseContext usingRetry(List<RetryPolicy<Object>> retryPolicies) {
        retryPolicy = Failsafe.with(retryPolicies);
        return this;
    }

    @Override
    public void breakLease(String entityId) {
        writeLease(entityId, false);
    }


    @Override
    public void acquireLease(String entityId) {
        writeLease(entityId, true);
    }

    private void writeLease(String entityId, boolean writeLease) {
        if (retryPolicy == null) {
            cosmosDbApi.invokeStoredProcedure(LEASE_SPROC_NAME, partitionKey, entityId, leaseHolder, writeLease);
        } else {
            retryPolicy.run(() -> cosmosDbApi.invokeStoredProcedure(LEASE_SPROC_NAME, partitionKey, entityId, leaseHolder, writeLease));
        }
    }
}
