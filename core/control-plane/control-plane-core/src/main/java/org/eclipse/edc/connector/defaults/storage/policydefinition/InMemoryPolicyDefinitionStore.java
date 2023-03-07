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

package org.eclipse.edc.connector.defaults.storage.policydefinition;

import org.eclipse.edc.connector.defaults.storage.ReflectionBasedQueryResolver;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.util.concurrency.LockManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * An in-memory, threadsafe policy store. This implementation is intended for testing purposes only.
 */
public class InMemoryPolicyDefinitionStore implements PolicyDefinitionStore {

    private final LockManager lockManager;
    private final Map<String, PolicyDefinition> policiesById = new HashMap<>();
    private final QueryResolver<PolicyDefinition> queryResolver = new ReflectionBasedQueryResolver<>(PolicyDefinition.class);

    public InMemoryPolicyDefinitionStore(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    @Override
    public PolicyDefinition findById(String policyId) {
        try {
            return lockManager.readLock(() -> policiesById.get(policyId));
        } catch (Exception e) {
            throw new EdcPersistenceException(format("Finding policy by id %s failed.", policyId), e);
        }
    }

    @Override
    public Stream<PolicyDefinition> findAll(QuerySpec spec) {
        try {
            return lockManager.readLock(() -> queryResolver.query(policiesById.values().stream(), spec));
        } catch (Exception e) {
            throw new EdcPersistenceException(format("Finding policy stream by query spec %s failed", spec), e);
        }
    }

    @Override
    public StoreResult<PolicyDefinition> save(PolicyDefinition policy) {
        try {
            return lockManager.writeLock(() -> {
                var id = policy.getUid();
                // do not replace if already exists
                if (policiesById.containsKey(id)) {
                    return StoreResult.alreadyExists(format(POLICY_ALREADY_EXISTS, id));
                }
                policiesById.put(id, policy);
                return StoreResult.success(policy);
            });
        } catch (Exception e) {
            throw new EdcPersistenceException("Saving policy failed", e);
        }
    }

    @Override
    public StoreResult<PolicyDefinition> update(PolicyDefinition policy) {
        try {
            var policyId = policy.getId();
            Objects.requireNonNull(policyId, "policyId");
            Objects.requireNonNull(policy, "policy");
            // do not update if not exists
            return lockManager.writeLock(() -> {
                if (policiesById.containsKey(policyId)) {
                    policiesById.put(policyId, policy);
                    return StoreResult.success(policy);
                }
                return StoreResult.notFound(format(POLICY_NOT_FOUND, policy));
            });

        } catch (Exception e) {
            throw new EdcPersistenceException("Updating policy failed", e);
        }
    }

    @Override
    public StoreResult<PolicyDefinition> deleteById(String policyId) {
        try {
            var previous = lockManager.writeLock(() -> policiesById.remove(policyId));
            return previous == null ?
                    StoreResult.notFound(format(POLICY_NOT_FOUND, policyId)) :
                    StoreResult.success(previous);
        } catch (Exception e) {
            throw new EdcPersistenceException("Deleting policy failed", e);
        }
    }

}
