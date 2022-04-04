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

package org.eclipse.dataspaceconnector.policy.store.memory;

import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.query.QueryResolver;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.ReflectionBasedQueryResolver;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * An in-memory, threadsafe policy store.
 * This implementation is intended for testing purposes only.
 */
public class InMemoryPolicyStore implements PolicyStore {

    private final LockManager lockManager;
    private final Map<String, Policy> policiesById = new HashMap<>();
    private final QueryResolver<Policy> queryResolver = new ReflectionBasedQueryResolver<>(Policy.class);

    public InMemoryPolicyStore(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    @Override
    public @Nullable Policy findById(String policyId) {
        try {
            return lockManager.readLock(() -> policiesById.get(policyId));
        } catch (Exception e) {
            throw new EdcPersistenceException(String.format("Finding policy by id %s failed.", policyId), e);
        }
    }

    @Override
    public Stream<Policy> findAll(QuerySpec spec) {
        try {
            return lockManager.readLock(() -> queryResolver.query(policiesById.values().stream(), spec));
        } catch (Exception e) {
            throw new EdcPersistenceException(String.format("Finding policy stream by query spec %s failed", spec), e);
        }
    }

    @Override
    public void save(Policy policy) {
        try {
            lockManager.writeLock(() -> policiesById.put(policy.getUid(), policy));
        } catch (Exception e) {
            throw new EdcPersistenceException("Saving policy failed", e);
        }
    }

    @Override
    public Policy deleteById(String policyId) {
        try {
            return lockManager.writeLock(() -> policiesById.remove(policyId));
        } catch (Exception e) {
            throw new EdcPersistenceException("Deleting policy failed", e);
        }
    }

}
