/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.core.scope.defaults;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.store.DcpScopeStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link DcpScopeStore} for testing and prototyping purposes.
 */
public class InMemoryDcpScopeStore implements DcpScopeStore {

    private final QueryResolver<DcpScope> queryResolver;

    private final Map<String, DcpScope> scopes = new ConcurrentHashMap<>();

    public InMemoryDcpScopeStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        this.queryResolver = new ReflectionBasedQueryResolver<>(DcpScope.class, criterionOperatorRegistry);
    }

    @Override
    public StoreResult<Void> save(DcpScope scope) {
        scopes.put(scope.getId(), scope);
        return StoreResult.success();
    }

    @Override
    public StoreResult<Void> delete(String scopeId) {
        scopes.remove(scopeId);
        return StoreResult.success();
    }

    @Override
    public StoreResult<List<DcpScope>> query(QuerySpec spec) {
        return StoreResult.success(queryResolver.query(scopes.values().stream(), spec)
                .collect(Collectors.toList()));
    }
}
