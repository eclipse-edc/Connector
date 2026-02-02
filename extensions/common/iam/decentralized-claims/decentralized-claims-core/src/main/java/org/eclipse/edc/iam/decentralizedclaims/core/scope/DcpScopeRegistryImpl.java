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
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;

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
    public ServiceResult<Void> remove(String scopeId) {
        return transactionContext.execute(() -> store.delete(scopeId).flatMap(ServiceResult::from));
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
