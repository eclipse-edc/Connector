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

import org.assertj.core.api.Assertions;
import org.eclipse.edc.policy.cel.engine.CelExpressionEngine;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.cel.store.CelExpressionStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class CelPolicyExpressionServiceImplTest {

    private final CelExpressionStore store = mock();
    private final CelExpressionEngine engine = mock();
    private final CelPolicyExpressionServiceImpl service = new CelPolicyExpressionServiceImpl(store, new NoopTransactionContext(), engine);

    @Test
    void create() {

        var expr = celExpression("expr1");
        when(engine.validate(anyString())).thenReturn(ServiceResult.success());
        when(store.create(expr)).thenReturn(StoreResult.success());

        var result = service.create(expr);

        assertThat(result).isSucceeded();
    }

    @Test
    void create_whenValidationFails() {

        var expr = celExpression("expr1");
        when(engine.validate(anyString())).thenReturn(ServiceResult.badRequest("invalid expression"));

        var result = service.create(expr);

        assertThat(result).isFailed();
        verifyNoInteractions(store);

    }

    @Test
    void create_whenStoreFails() {

        var expr = celExpression("expr1");
        when(engine.validate(anyString())).thenReturn(ServiceResult.success());
        when(store.create(expr)).thenReturn(StoreResult.generalError("store failure"));

        var result = service.create(expr);

        assertThat(result).isFailed();

    }

    @Test
    void update() {
        var expr = celExpression("expr1");
        when(engine.validate(anyString())).thenReturn(ServiceResult.success());
        when(store.update(expr)).thenReturn(StoreResult.success());

        var result = service.update(expr);

        assertThat(result).isSucceeded();
    }

    @Test
    void update_whenValidationFails() {

        var expr = celExpression("expr1");
        when(engine.validate(anyString())).thenReturn(ServiceResult.badRequest("invalid expression"));

        var result = service.update(expr);

        assertThat(result).isFailed();
        verifyNoInteractions(store);
    }

    @Test
    void update_whenStoreFails() {

        var expr = celExpression("expr1");
        when(engine.validate(anyString())).thenReturn(ServiceResult.success());
        when(store.update(expr)).thenReturn(StoreResult.generalError("store failure"));
        var result = service.update(expr);

        assertThat(result).isFailed();
    }

    @Test
    void query() {

        when(store.query(any())).thenReturn(List.of(celExpression("id1"), celExpression("id2")));

        var result = service.query(QuerySpec.max());

        assertThat(result).isSucceeded().satisfies(celExpressions -> {
            Assertions.assertThat(celExpressions).hasSize(2);
        });
    }

    @Test
    void delete() {

        when(store.delete("id")).thenReturn(StoreResult.success());

        var result = service.delete("id");

        assertThat(result).isSucceeded();
    }

    private CelExpression celExpression(String id) {
        return CelExpression.Builder.newInstance().id(id)
                .leftOperand("leftOperand")
                .expression("expression")
                .description("description")
                .build();
    }
}
