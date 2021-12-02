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

import de.fraunhofer.iais.eis.ConstraintBuilder;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.AndConstraint;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.MultiplicityConstraint;
import org.eclipse.dataspaceconnector.policy.model.OrConstraint;
import org.eclipse.dataspaceconnector.policy.model.XoneConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.policy.model.Operator.EQ;

class ConstraintToIdsLogicalConstraintTransformerTest {
    private ConstraintToIdsLogicalConstraintTransformer transformer;
    private TransformerContext context;

    @Test
    void verifyAndConstraint() {
        EasyMock.expect(context.transform(EasyMock.isA(IdsId.class), EasyMock.eq(URI.class))).andReturn(URI.create("foo"));
        EasyMock.expect(context.transform(EasyMock.isA(AtomicConstraint.class), EasyMock.eq(de.fraunhofer.iais.eis.Constraint.class)))
                .andReturn(new ConstraintBuilder().build()).times(2);
        EasyMock.replay(context);

        var atomicConstraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("1"))
                .operator(EQ)
                .rightExpression(new LiteralExpression("2"))
                .build();
        var andConstraint = AndConstraint.Builder.newInstance().constraint(atomicConstraint).constraint(atomicConstraint).build();

        var transformed = (MultiplicityConstraint) transformer.transform(andConstraint, context);
        assertThat(transformed).isInstanceOf(AndConstraint.class);
        assertThat(transformed.getConstraints()).isNotNull();

        EasyMock.verify(context);
    }


    @Test
    void verifyOrConstraint() {
        EasyMock.expect(context.transform(EasyMock.isA(IdsId.class), EasyMock.eq(URI.class))).andReturn(URI.create("foo"));
        EasyMock.expect(context.transform(EasyMock.isA(AtomicConstraint.class), EasyMock.eq(de.fraunhofer.iais.eis.Constraint.class)))
                .andReturn(new ConstraintBuilder().build()).times(2);
        EasyMock.replay(context);

        var atomicConstraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("1"))
                .operator(EQ)
                .rightExpression(new LiteralExpression("2"))
                .build();
        var orConstraint = OrConstraint.Builder.newInstance().constraint(atomicConstraint).constraint(atomicConstraint).build();

        var transformed = transformer.transform(orConstraint, context);
        assertThat(transformed).isNotNull();
        assertThat(transformed.getOr()).isNotNull();

        EasyMock.verify(context);
    }


    @Test
    void verifyXoneConstraint() {
        EasyMock.expect(context.transform(EasyMock.isA(IdsId.class), EasyMock.eq(URI.class))).andReturn(URI.create("foo"));
        EasyMock.expect(context.transform(EasyMock.isA(AtomicConstraint.class), EasyMock.eq(de.fraunhofer.iais.eis.Constraint.class)))
                .andReturn(new ConstraintBuilder().build()).times(2);
        EasyMock.replay(context);

        var atomicConstraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("1"))
                .operator(EQ)
                .rightExpression(new LiteralExpression("2"))
                .build();
        var xoneConstraint = XoneConstraint.Builder.newInstance().constraint(atomicConstraint).constraint(atomicConstraint).build();

        var transformed = transformer.transform(xoneConstraint, context);
        assertThat(transformed).isNotNull();
        assertThat(transformed.getXone()).isNotNull();

        EasyMock.verify(context);
    }


    @BeforeEach
    void setUp() {
        transformer = new ConstraintToIdsLogicalConstraintTransformer();
        context = EasyMock.createMock(TransformerContext.class);
    }
}
