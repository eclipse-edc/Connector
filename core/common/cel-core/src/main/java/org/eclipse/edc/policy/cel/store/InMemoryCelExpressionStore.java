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

package org.eclipse.edc.policy.cel.store;

import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryCelExpressionStore implements CelExpressionStore {

    private final QueryResolver<CelExpression> queryResolver;
    private final Map<String, CelExpression> expressions = new ConcurrentHashMap<>();

    public InMemoryCelExpressionStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        this.queryResolver = new ReflectionBasedQueryResolver<>(CelExpression.class, criterionOperatorRegistry);
    }

    @Override
    public StoreResult<Void> create(CelExpression expression) {
        return expressions.putIfAbsent(expression.getId(), expression) == null
                ? StoreResult.success()
                : StoreResult.alreadyExists(alreadyExistsErrorMessage(expression.getId()));
    }

    @Override
    public StoreResult<Void> update(CelExpression expression) {
        return expressions.replace(expression.getId(), expression) != null
                ? StoreResult.success()
                : StoreResult.notFound(notFoundErrorMessage(expression.getId()));
    }

    @Override
    public StoreResult<Void> delete(String id) {
        return expressions.remove(id) != null
                ? StoreResult.success()
                : StoreResult.notFound(notFoundErrorMessage(id));
    }

    @Override
    public List<CelExpression> query(QuerySpec querySpec) {
        return queryResolver.query(expressions.values().stream(), querySpec)
                .collect(Collectors.toList());
    }
}
