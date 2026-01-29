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

package org.eclipse.edc.policy.cel.service;

import org.eclipse.edc.policy.cel.engine.CelExpressionEngine;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.cel.store.CelExpressionStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;

public class CelPolicyExpressionServiceImpl implements CelPolicyExpressionService {

    private final CelExpressionStore store;
    private final TransactionContext tx;
    private final CelExpressionEngine engine;

    public CelPolicyExpressionServiceImpl(CelExpressionStore store, TransactionContext tx, CelExpressionEngine engine) {
        this.store = store;
        this.tx = tx;
        this.engine = engine;
    }

    @Override
    public ServiceResult<Void> create(CelExpression expression) {
        return tx.execute(() -> {
            var validationResult = engine.validate(expression.getExpression());
            if (validationResult.failed()) {
                return validationResult;
            }
            var result = store.create(expression);
            if (result.succeeded()) {
                return ServiceResult.success();
            } else {
                return ServiceResult.from(result);
            }
        });
    }

    @Override
    public ServiceResult<CelExpression> findById(String id) {
        return tx.execute(() -> {
            var query = QuerySpec.Builder.newInstance().filter(Criterion.criterion("id", "=", id)).build();
            var results = store.query(query);
            if (results.isEmpty()) {
                return ServiceResult.notFound("CelExpression with id " + id + " not found");
            } else {
                return ServiceResult.success(results.get(0));
            }
        });
    }

    @Override
    public ServiceResult<Void> update(CelExpression expression) {
        return tx.execute(() -> {
            var validationResult = engine.validate(expression.getExpression());
            if (validationResult.failed()) {
                return validationResult;
            }
            var result = store.update(expression);
            if (result.succeeded()) {
                return ServiceResult.success();
            } else {
                return ServiceResult.from(result);
            }
        });
    }

    @Override
    public ServiceResult<Void> delete(String id) {
        return tx.execute(() -> store.delete(id)
                .flatMap(ServiceResult::from));
    }

    @Override
    public ServiceResult<List<CelExpression>> query(QuerySpec querySpec) {
        return ServiceResult.success(store.query(querySpec));
    }
}
