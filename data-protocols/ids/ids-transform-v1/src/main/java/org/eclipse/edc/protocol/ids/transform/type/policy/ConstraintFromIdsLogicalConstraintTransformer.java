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
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.transform.type.policy;

import de.fraunhofer.iais.eis.LogicalConstraint;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.util.collection.CollectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ConstraintFromIdsLogicalConstraintTransformer implements IdsTypeTransformer<LogicalConstraint, Constraint> {

    @Override
    public Class<LogicalConstraint> getInputType() {
        return LogicalConstraint.class;
    }

    @Override
    public Class<Constraint> getOutputType() {
        return Constraint.class;
    }

    @Override
    public @Nullable Constraint transform(LogicalConstraint constraint, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (constraint == null) {
            return null;
        }

        int constraintTypes = 0;
        if (CollectionUtil.isNotEmpty(constraint.getAnd())) {
            constraintTypes++;
        }

        if (CollectionUtil.isNotEmpty(constraint.getOr())) {
            constraintTypes++;
        }

        if (CollectionUtil.isNotEmpty(constraint.getXone())) {
            constraintTypes++;
        }

        if (constraintTypes == 0) {
            context.reportProblem("Logical constraint must specify at least AND, OR, or XONE constraints");
            return null;
        } else if (constraintTypes == 1) {
            return transformSingleConstraintType(constraint, context);
        } else {
            return transformMultipleConstraintTypes(constraint, context);
        }
    }

    /**
     * Transforms a set of AND, OR, or XONE constraint types.
     */
    private MultiplicityConstraint transformSingleConstraintType(LogicalConstraint constraint, @NotNull TransformerContext context) {
        if (CollectionUtil.isNotEmpty(constraint.getAnd())) {
            var builder = AndConstraint.Builder.newInstance();
            constraint.getAnd().forEach(subConstraint -> builder.constraint(context.transform(subConstraint, Constraint.class)));
            return builder.build();
        } else if (CollectionUtil.isNotEmpty(constraint.getOr())) {
            var builder = OrConstraint.Builder.newInstance();
            constraint.getOr().forEach(subConstraint -> builder.constraint(context.transform(subConstraint, Constraint.class)));
            return builder.build();
        } else {
            var builder = XoneConstraint.Builder.newInstance();
            constraint.getXone().forEach(subConstraint -> builder.constraint(context.transform(subConstraint, Constraint.class)));
            return builder.build();
        }
    }

    /**
     * If more than one AND, OR, or XONE constraint is specified, return an AND Constraint with contained
     * child constraints matching the logical constraint type.
     */
    private MultiplicityConstraint transformMultipleConstraintTypes(LogicalConstraint constraint, @NotNull TransformerContext context) {
        var rootBuilder = AndConstraint.Builder.newInstance();
        if (CollectionUtil.isNotEmpty(constraint.getAnd())) {
            var builder = AndConstraint.Builder.newInstance();
            constraint.getAnd().forEach(subConstraint -> builder.constraint(context.transform(subConstraint, Constraint.class)));
            rootBuilder.constraint(builder.build());
        }
        if (CollectionUtil.isNotEmpty(constraint.getOr())) {
            var builder = OrConstraint.Builder.newInstance();
            constraint.getOr().forEach(subConstraint -> builder.constraint(context.transform(subConstraint, Constraint.class)));
            rootBuilder.constraint(builder.build());
        }
        if (CollectionUtil.isNotEmpty(constraint.getOr())) {
            var builder = XoneConstraint.Builder.newInstance();
            constraint.getXone().forEach(subConstraint -> builder.constraint(context.transform(subConstraint, Constraint.class)));
            rootBuilder.constraint(builder.build());
        }
        return rootBuilder.build();
    }
}
