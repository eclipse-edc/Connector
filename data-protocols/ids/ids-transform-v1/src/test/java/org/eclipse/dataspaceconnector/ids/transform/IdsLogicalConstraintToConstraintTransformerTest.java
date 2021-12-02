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

import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.ConstraintBuilder;
import de.fraunhofer.iais.eis.LeftOperand;
import de.fraunhofer.iais.eis.LogicalConstraintBuilder;
import de.fraunhofer.iais.eis.util.RdfResource;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.AndConstraint;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.MultiplicityConstraint;
import org.eclipse.dataspaceconnector.policy.model.OrConstraint;
import org.eclipse.dataspaceconnector.policy.model.XoneConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdsLogicalConstraintToConstraintTransformerTest {
    private IdsLogicalConstraintToConstraintTransformer transformer;
    private TransformerContext context;

    @Test
    void verifyAndConstraint() {
        EasyMock.expect(context.transform(EasyMock.isA(de.fraunhofer.iais.eis.Constraint.class), EasyMock.eq(Constraint.class)))
                .andReturn(AtomicConstraint.Builder.newInstance().build());
        EasyMock.replay(context);

        var constraint = new ConstraintBuilder()
                ._leftOperand_(LeftOperand.COUNT)
                ._operator_(BinaryOperator.EQ)
                ._rightOperand_(new RdfResource())
                .build();
        var logicalConstraint = new LogicalConstraintBuilder()._and_(constraint).build();


        var transformed = transformer.transform(logicalConstraint, context);

        assertThat(transformed).isInstanceOf(AndConstraint.class);
        assertThat(((MultiplicityConstraint) transformed).getConstraints()).isNotEmpty();

        EasyMock.verify(context);
    }

    @Test
    void verifyOrConstraint() {
        EasyMock.expect(context.transform(EasyMock.isA(de.fraunhofer.iais.eis.Constraint.class), EasyMock.eq(Constraint.class)))
                .andReturn(AtomicConstraint.Builder.newInstance().build());
        EasyMock.replay(context);

        var constraint = new ConstraintBuilder()
                ._leftOperand_(LeftOperand.COUNT)
                ._operator_(BinaryOperator.EQ)
                ._rightOperand_(new RdfResource())
                .build();
        var logicalConstraint = new LogicalConstraintBuilder()._or_(constraint).build();


        var transformed = transformer.transform(logicalConstraint, context);

        assertThat(transformed).isInstanceOf(OrConstraint.class);
        assertThat(((MultiplicityConstraint) transformed).getConstraints()).isNotEmpty();

        EasyMock.verify(context);
    }

    @Test
    void verifyXoneConstraint() {
        EasyMock.expect(context.transform(EasyMock.isA(de.fraunhofer.iais.eis.Constraint.class), EasyMock.eq(Constraint.class)))
                .andReturn(AtomicConstraint.Builder.newInstance().build());
        EasyMock.replay(context);

        var constraint = new ConstraintBuilder()
                ._leftOperand_(LeftOperand.COUNT)
                ._operator_(BinaryOperator.EQ)
                ._rightOperand_(new RdfResource())
                .build();
        var logicalConstraint = new LogicalConstraintBuilder()._xone_(constraint).build();


        var transformed = transformer.transform(logicalConstraint, context);

        assertThat(transformed).isInstanceOf(XoneConstraint.class);
        assertThat(((MultiplicityConstraint) transformed).getConstraints()).isNotEmpty();

        EasyMock.verify(context);
    }

    @BeforeEach
    void setUp() {
        transformer = new IdsLogicalConstraintToConstraintTransformer();
        context = EasyMock.createMock(TransformerContext.class);
    }

}
