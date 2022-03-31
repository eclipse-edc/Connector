/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.LogicalConstraint;
import org.eclipse.dataspaceconnector.ids.core.policy.IdsConstraintBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.policy.model.AndConstraint;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.OrConstraint;
import org.eclipse.dataspaceconnector.policy.model.XoneConstraint;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.policy.model.Operator.EQ;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConstraintToIdsLogicalConstraintTransformerTest {
    private ConstraintToIdsLogicalConstraintTransformer transformer;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ConstraintToIdsLogicalConstraintTransformer();
        context = mock(TransformerContext.class);
    }

    @Test
    void verifyAndConstraint() {
        when(context.transform(isA(IdsId.class), eq(URI.class))).thenReturn(URI.create("foo"));
        when(context.transform(isA(AtomicConstraint.class), eq(de.fraunhofer.iais.eis.Constraint.class)))
                .thenReturn(new IdsConstraintBuilder().build());

        var atomicConstraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("1"))
                .operator(EQ)
                .rightExpression(new LiteralExpression("2"))
                .build();
        var andConstraint = AndConstraint.Builder.newInstance().constraint(atomicConstraint).constraint(atomicConstraint).build();

        var transformed = (LogicalConstraint) transformer.transform(andConstraint, context);

        assertThat(transformed.getAnd()).isNotEmpty();
        verify(context, times(3)).transform(any(), any());
    }

    @Test
    void verifyOrConstraint() {
        when(context.transform(isA(IdsId.class), eq(URI.class))).thenReturn(URI.create("foo"));
        when(context.transform(isA(AtomicConstraint.class), eq(de.fraunhofer.iais.eis.Constraint.class)))
                .thenReturn(new IdsConstraintBuilder().build());

        var atomicConstraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("1"))
                .operator(EQ)
                .rightExpression(new LiteralExpression("2"))
                .build();
        var orConstraint = OrConstraint.Builder.newInstance().constraint(atomicConstraint).constraint(atomicConstraint).build();

        var transformed = transformer.transform(orConstraint, context);

        assertThat(transformed).isNotNull();
        assertThat(transformed.getOr()).isNotNull();
        verify(context, times(3)).transform(any(), any());

    }


    @Test
    void verifyXoneConstraint() {
        when(context.transform(isA(IdsId.class), eq(URI.class))).thenReturn(URI.create("foo"));
        when(context.transform(isA(AtomicConstraint.class), eq(de.fraunhofer.iais.eis.Constraint.class)))
                .thenReturn(new IdsConstraintBuilder().build());

        var atomicConstraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("1"))
                .operator(EQ)
                .rightExpression(new LiteralExpression("2"))
                .build();
        var xoneConstraint = XoneConstraint.Builder.newInstance().constraint(atomicConstraint).constraint(atomicConstraint).build();

        var transformed = transformer.transform(xoneConstraint, context);

        assertThat(transformed).isNotNull();
        assertThat(transformed.getXone()).isNotNull();
        verify(context, times(3)).transform(any(), any());
    }

}
