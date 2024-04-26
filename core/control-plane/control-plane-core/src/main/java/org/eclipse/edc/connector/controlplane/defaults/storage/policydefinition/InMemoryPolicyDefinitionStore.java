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

package org.eclipse.edc.connector.controlplane.defaults.storage.policydefinition;

import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * An in-memory, threadsafe policy store. This implementation is intended for testing purposes only.
 */
public class InMemoryPolicyDefinitionStore implements PolicyDefinitionStore {

    private final Map<String, PolicyDefinition> policiesById = new ConcurrentHashMap<>();
    private final QueryResolver<PolicyDefinition> queryResolver;

    public InMemoryPolicyDefinitionStore(CriterionOperatorRegistry criterionToPredicateConverter) {
        queryResolver = new ReflectionBasedQueryResolver<>(PolicyDefinition.class, criterionToPredicateConverter);
    }

    @Override
    public PolicyDefinition findById(String policyId) {
        try {
            return policiesById.get(policyId);
        } catch (Exception e) {
            throw new EdcPersistenceException(format("Finding policy by id %s failed.", policyId), e);
        }
    }

    @Override
    public Stream<PolicyDefinition> findAll(QuerySpec spec) {
        return queryResolver.query(policiesById.values().stream(), spec);
    }

    @Override
    public StoreResult<PolicyDefinition> create(PolicyDefinition policy) {
        var prev = policiesById.putIfAbsent(policy.getId(), policy);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.<PolicyDefinition>alreadyExists(format(POLICY_ALREADY_EXISTS, policy.getId())))
                .orElse(StoreResult.success(policy));

    }

    @Override
    public StoreResult<PolicyDefinition> update(PolicyDefinition policy) {
        var prev = policiesById.replace(policy.getId(), policy);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.success(policy))
                .orElse(StoreResult.notFound(format(POLICY_NOT_FOUND, policy.getId())));
    }

    @Override
    public StoreResult<PolicyDefinition> delete(String policyId) {
        var prev = policiesById.remove(policyId);
        return Optional.ofNullable(prev)
                .map(StoreResult::success)
                .orElse(StoreResult.notFound(format(POLICY_NOT_FOUND, policyId)));
    }

}
