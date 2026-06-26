/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.iam.decentralizedclaims.core.scope;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.store.DcpScopeStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;

import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * Implementation of {@link DcpScopeRegistry}.
 */
public class DcpScopeRegistryImpl implements DcpScopeRegistry {

    private final TransactionContext transactionContext;
    private final DcpScopeStore store;

    public DcpScopeRegistryImpl(TransactionContext transactionContext, DcpScopeStore store) {
        this.transactionContext = transactionContext;
        this.store = store;
    }

    @Override
    public ServiceResult<Void> register(DcpScope scope) {
        return transactionContext.execute(() -> store.save(scope).flatMap(ServiceResult::from));
    }

    @Override
    public ServiceResult<Void> create(DcpScope scope) {
        return transactionContext.execute(() -> findById(scope.getId())
                .compose(existing -> existing.isEmpty()
                        ? store.save(scope)
                        : StoreResult.<Void>alreadyExists("DcpScope with id %s already exists".formatted(scope.getId())))
                .flatMap(ServiceResult::from));
    }

    @Override
    public ServiceResult<Void> update(DcpScope scope) {
        return transactionContext.execute(() -> findById(scope.getId())
                .compose(existing -> existing.isEmpty()
                        ? StoreResult.<Void>notFound("DcpScope with id %s does not exist".formatted(scope.getId()))
                        : store.save(scope))
                .flatMap(ServiceResult::from));
    }

    @Override
    public ServiceResult<List<DcpScope>> query(QuerySpec spec) {
        return transactionContext.execute(() -> store.query(spec).flatMap(ServiceResult::from));
    }

    @Override
    public ServiceResult<Void> remove(String scopeId) {
        return transactionContext.execute(() -> store.delete(scopeId).flatMap(ServiceResult::from));
    }

    private StoreResult<List<DcpScope>> findById(String id) {
        var query = QuerySpec.Builder.newInstance()
                .filter(criterion("id", "=", id))
                .build();
        return store.query(query);
    }

    @Override
    public ServiceResult<List<DcpScope>> getDefaultScopes() {
        var query = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("type", "=", DcpScope.Type.DEFAULT.name()))
                .build();
        return transactionContext.execute(() -> store.query(query).flatMap(ServiceResult::from));
    }

    @Override
    public ServiceResult<List<DcpScope>> getScopeMapping() {
        var query = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("type", "=", DcpScope.Type.POLICY.name()))
                .build();
        return transactionContext.execute(() -> store.query(query).flatMap(ServiceResult::from));
    }
}
