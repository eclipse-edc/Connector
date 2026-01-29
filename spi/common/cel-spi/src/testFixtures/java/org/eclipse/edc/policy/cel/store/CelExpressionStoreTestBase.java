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

package org.eclipse.edc.policy.cel.store;

import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public abstract class CelExpressionStoreTestBase {

    protected abstract CelExpressionStore getStore();

    @Test
    void create() {
        var expr = celExpression("expr1");
        var result = getStore().create(expr);
        assertThat(result).isSucceeded();
        var expressions = getStore().query(QuerySpec.max());
        assertThat(expressions).usingRecursiveFieldByFieldElementComparator().containsExactly(expr);
    }

    @Test
    void create_whenExists_shouldReturnFailure() {
        var expr = celExpression("expr1");
        var result = getStore().create(expr);
        assertThat(result).isSucceeded();
        var result2 = getStore().create(expr);

        assertThat(result2).isFailed().detail().contains("already exists");
    }

    @Test
    void query_noQuerySpec() {
        var resources = range(0, 5)
                .mapToObj(i -> celExpression("id" + i))
                .toList();

        resources.forEach(getStore()::create);

        var res = getStore().query(QuerySpec.none());
        assertThat(res)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(resources.toArray(new CelExpression[0]));
    }

    @Test
    void query_leftOperand() {
        var resources = range(0, 5)
                .mapToObj(i -> celExpression("id" + i, "leftOperand" + i))
                .toList();

        resources.forEach(getStore()::create);

        var res = getStore().query(QuerySpec.Builder.newInstance().filter(Criterion.criterion("leftOperand", "=", "leftOperand3")).build());
        assertThat(res).hasSize(1)
                .first().satisfies(celExpression -> {
                    assertThat(celExpression).usingRecursiveComparison().isEqualTo(resources.get(3));
                });
    }

    @Test
    void query_whenNotFound() {
        var resources = range(0, 5)
                .mapToObj(i -> celExpression("id" + i))
                .toList();

        resources.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance().filter(Criterion.criterion("id", "=", "non-existing-id"))
                .build();
        var res = getStore().query(query);
        assertThat(res).isEmpty();
    }

    @Test
    void query_byInvalidField_shouldReturnEmptyList() {
        var resources = range(0, 5)
                .mapToObj(i -> celExpression("id" + i))
                .toList();


        resources.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("invalidField", "=", "test-value"))
                .build();
        var res = getStore().query(query);
        assertThat(res).isNotNull().isEmpty();
    }

    @Test
    void update() {
        var expr = celExpression("expr");
        var result = getStore().create(expr);
        assertThat(result).isSucceeded();

        var newExpr = celExpression("expr");
        var updateRes = getStore().update(newExpr);
        assertThat(updateRes).isSucceeded();

        assertThat(getStore().query(QuerySpec.max()))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(newExpr);
    }

    @Test
    void update_whenNotExists() {
        var expr = celExpression("another-id");

        var updateRes = getStore().update(expr);
        assertThat(updateRes).isFailed().detail().contains("with ID 'another-id' does not exist.");
    }

    @Test
    void delete() {
        var expr = celExpression("expr-to-delete");
        getStore().create(expr);

        var deleteRes = getStore().delete(expr.getId());
        assertThat(deleteRes).isSucceeded();
    }

    @Test
    void delete_whenNotExists() {
        assertThat(getStore().delete("not-exist")).isFailed()
                .detail().contains("with ID 'not-exist' does not exist.");
    }

    private CelExpression celExpression(String id) {
        return celExpression(id, "leftOperand");
    }

    private CelExpression celExpression(String id, String leftOperand) {
        return CelExpression.Builder.newInstance().id(id)
                .leftOperand(leftOperand)
                .expression("expression")
                .description("description")
                .build();
    }
}
