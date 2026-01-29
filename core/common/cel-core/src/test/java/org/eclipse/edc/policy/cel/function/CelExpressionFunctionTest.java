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

package org.eclipse.edc.policy.cel.function;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.cel.engine.CelExpressionEngine;
import org.eclipse.edc.policy.cel.function.context.CelContextMapper;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CelExpressionFunctionTest {

    private final CelExpressionEngine engine = mock();

    private final ParticipantAgentPolicyContext policyContext = mock();
    private final CelContextMapper<ParticipantAgentPolicyContext> contextSupplier = mock();
    private final CelExpressionFunction<ParticipantAgentPolicyContext, Permission> function = new CelExpressionFunction<>(engine, contextSupplier);

    @Test
    void evaluate() {

        when(contextSupplier.mapContext(any())).thenReturn(Result.success(Map.of()));
        when(engine.evaluateExpression(any(), any(), any(), any())).thenReturn(ServiceResult.success(true));

        var result = function.evaluate("arg1", Operator.EQ, "arg2", null, policyContext);

        assertThat(result).isTrue();

    }

    @Test
    void evaluate_whenContextResolveFails() {

        when(contextSupplier.mapContext(any())).thenReturn(Result.failure("Failed"));

        var result = function.evaluate("arg1", Operator.EQ, "arg2", null, policyContext);

        assertThat(result).isFalse();

        verify(policyContext).reportProblem(any());
    }

    @Test
    void evaluate_evaluationFails() {
        when(contextSupplier.mapContext(any())).thenReturn(Result.success(Map.of()));
        when(engine.evaluateExpression(any(), any(), any(), any())).thenReturn(ServiceResult.badRequest("Failed"));

        var result = function.evaluate("arg1", Operator.EQ, "arg2", null, policyContext);

        assertThat(result).isFalse();

        verify(policyContext).reportProblem(any());
    }

    @Test
    void canHandle() {
        when(engine.canEvaluate(any())).thenReturn(true);

        var result = function.canHandle("arg1");

        assertThat(result).isTrue();

    }
}
