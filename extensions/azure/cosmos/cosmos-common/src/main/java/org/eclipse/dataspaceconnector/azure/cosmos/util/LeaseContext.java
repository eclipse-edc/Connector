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

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeExecutor;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApi;

import java.util.List;
import java.util.Objects;

/**
 * Provides document-agnostic functionality to acquire and break an exclusive lock on the database level by calling a stored
 * procedure.
 *
 * <em>This happens on the database in an ACID way.</em>
 */
public class LeaseContext {
    private static final String LEASE_SPROC_NAME = "lease";
    private final String partitionKey;
    private final CosmosDbApi cosmosDbApi;
    private final String leaseHolder;
    private FailsafeExecutor<Object> retryPolicy;

    private LeaseContext(CosmosDbApi cosmosDbApi, String partitionKey, String leaseHolder) {
        this.cosmosDbApi = cosmosDbApi;
        this.partitionKey = partitionKey;
        this.leaseHolder = leaseHolder;
    }

    /**
     * Creates a new instance of the {@link LeaseContext} class
     *
     * @param api          An instance of the {@link CosmosDbApi}
     * @param leaseHolder  A string identifying the holder of the lease, e.g. a connector runtime name
     * @param partitionKey The partition key
     */
    public static LeaseContext with(CosmosDbApi api, String partitionKey, String leaseHolder) {
        Objects.requireNonNull(api, "CosmosDbApi");
        Objects.requireNonNull(partitionKey, "partitionKey");
        Objects.requireNonNull(leaseHolder, "leaseHolder");
        return new LeaseContext(api, partitionKey, leaseHolder);
    }

    /**
     * Optionally provide a {@link RetryPolicy} to guard agains transient errors.
     */
    public LeaseContext usingRetry(List<RetryPolicy<Object>> retryPolicies) {
        retryPolicy = Failsafe.with(retryPolicies);
        return this;
    }

    /**
     * Breaks the exclusive Lock on a document
     *
     * @param documentId The database ID of the document
     * @throws com.azure.cosmos.implementation.BadRequestException if the lease could not be broken, e.g. because another holder holds it.
     */
    public void breakLease(String documentId) {
        writeLease(documentId, false);
    }

    /**
     * Acquires the exclusive Lock on a document
     *
     * @param documentId The database ID of the document
     * @throws com.azure.cosmos.implementation.BadRequestException if the lease could not be acquired, e.g. because another holder holds it.
     */
    public void acquireLease(String documentId) {
        writeLease(documentId, true);
    }

    private void writeLease(String documentId, boolean writeLease) {
        if (retryPolicy == null) {
            cosmosDbApi.invokeStoredProcedure(LEASE_SPROC_NAME, partitionKey, documentId, leaseHolder, writeLease);
        } else {
            retryPolicy.run(() -> cosmosDbApi.invokeStoredProcedure(LEASE_SPROC_NAME, partitionKey, documentId, leaseHolder, writeLease));
        }
    }
}
