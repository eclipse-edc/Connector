/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *       Fraunhofer Insitute for Software and Systems Engineering
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.util.RdfResource;
import org.eclipse.dataspaceconnector.ids.core.policy.IdsConstraintBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

public class ConstraintToIdsConstraintTransformer implements IdsTypeTransformer<Constraint, de.fraunhofer.iais.eis.Constraint> {

    @Override
    public Class<Constraint> getInputType() {
        return Constraint.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.Constraint> getOutputType() {
        return de.fraunhofer.iais.eis.Constraint.class;
    }

    @Override
    public @Nullable de.fraunhofer.iais.eis.Constraint transform(Constraint constraint, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (constraint == null) {
            return null;
        }

        if (constraint instanceof AtomicConstraint) {
            return transformAtomicConstraint((AtomicConstraint) constraint, context);
        } else {
            context.reportProblem(String.format("An IDS constraint requires an AtomicConstraint source: %s", constraint.getClass().getName()));
            return null;
        }

    }

    private de.fraunhofer.iais.eis.Constraint transformAtomicConstraint(AtomicConstraint atomicConstraint, @NotNull TransformerContext context) {
        var leftOperand = context.transform(atomicConstraint.getLeftExpression(), String.class);
        var rightOperand = context.transform(atomicConstraint.getRightExpression(), RdfResource.class);
        var operator = context.transform(atomicConstraint.getOperator(), BinaryOperator.class);

        var idsId = IdsId.Builder.newInstance().value(atomicConstraint.hashCode()).type(IdsType.CONSTRAINT).build();
        var id = context.transform(idsId, URI.class);
        var constraintBuilder = new IdsConstraintBuilder(id);

        constraintBuilder.leftOperand(leftOperand);
        constraintBuilder.rightOperand(rightOperand);
        constraintBuilder.operator(operator);

        return constraintBuilder.build();
    }


}
