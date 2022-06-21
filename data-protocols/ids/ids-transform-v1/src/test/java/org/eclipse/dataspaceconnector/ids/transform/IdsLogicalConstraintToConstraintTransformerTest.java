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
import de.fraunhofer.iais.eis.LogicalConstraintBuilder;
import de.fraunhofer.iais.eis.util.RdfResource;
import org.eclipse.dataspaceconnector.ids.core.policy.IdsConstraintBuilder;
import org.eclipse.dataspaceconnector.policy.model.AndConstraint;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.MultiplicityConstraint;
import org.eclipse.dataspaceconnector.policy.model.OrConstraint;
import org.eclipse.dataspaceconnector.policy.model.XoneConstraint;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdsLogicalConstraintToConstraintTransformerTest {
    private IdsLogicalConstraintToConstraintTransformer transformer;
    private TransformerContext context;

    @Test
    void verifyAndConstraint() {
        when(context.transform(isA(de.fraunhofer.iais.eis.Constraint.class), eq(Constraint.class)))
                .thenReturn(AtomicConstraint.Builder.newInstance().build());

        var constraint = new IdsConstraintBuilder()
                .leftOperand("COUNT")
                .operator(BinaryOperator.EQ)
                .rightOperand(new RdfResource())
                .build();
        var logicalConstraint = new LogicalConstraintBuilder()._and_(constraint).build();


        var transformed = transformer.transform(logicalConstraint, context);

        assertThat(transformed).isInstanceOf(AndConstraint.class);
        assertThat(((MultiplicityConstraint) transformed).getConstraints()).isNotEmpty();
        verify(context).transform(isA(de.fraunhofer.iais.eis.Constraint.class), eq(Constraint.class));
    }

    @Test
    void verifyOrConstraint() {
        when(context.transform(isA(de.fraunhofer.iais.eis.Constraint.class), eq(Constraint.class)))
                .thenReturn(AtomicConstraint.Builder.newInstance().build());

        var constraint = new IdsConstraintBuilder()
                .leftOperand("COUNT")
                .operator(BinaryOperator.EQ)
                .rightOperand(new RdfResource())
                .build();
        var logicalConstraint = new LogicalConstraintBuilder()._or_(constraint).build();


        var transformed = transformer.transform(logicalConstraint, context);

        assertThat(transformed).isInstanceOf(OrConstraint.class);
        assertThat(((MultiplicityConstraint) transformed).getConstraints()).isNotEmpty();
        verify(context).transform(isA(de.fraunhofer.iais.eis.Constraint.class), eq(Constraint.class));
    }

    @Test
    void verifyXoneConstraint() {
        when(context.transform(isA(de.fraunhofer.iais.eis.Constraint.class), eq(Constraint.class)))
                .thenReturn(AtomicConstraint.Builder.newInstance().build());

        var constraint = new IdsConstraintBuilder()
                .leftOperand("COUNT")
                .operator(BinaryOperator.EQ)
                .rightOperand(new RdfResource())
                .build();
        var logicalConstraint = new LogicalConstraintBuilder()._xone_(constraint).build();


        var transformed = transformer.transform(logicalConstraint, context);

        assertThat(transformed).isInstanceOf(XoneConstraint.class);
        assertThat(((MultiplicityConstraint) transformed).getConstraints()).isNotEmpty();
        verify(context).transform(isA(de.fraunhofer.iais.eis.Constraint.class), eq(Constraint.class));
    }

    @BeforeEach
    void setUp() {
        transformer = new IdsLogicalConstraintToConstraintTransformer();
        context = mock(TransformerContext.class);
    }

}
