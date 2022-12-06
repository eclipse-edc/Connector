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

import de.fraunhofer.iais.eis.LogicalConstraintBuilder;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Transforms an EDC constraint to its corresponding IDS type. Specifically, an EDC {@link MultiplicityConstraint} is transformed to a
 * {@link de.fraunhofer.iais.eis.LogicalConstraint}; an EDC {@link AtomicConstraint} is transformed to a {@link de.fraunhofer.iais.eis.Constraint}.
 */
public class ConstraintToIdsLogicalConstraintTransformer implements IdsTypeTransformer<Constraint, de.fraunhofer.iais.eis.LogicalConstraint> {

    @Override
    public Class<Constraint> getInputType() {
        return Constraint.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.LogicalConstraint> getOutputType() {
        return de.fraunhofer.iais.eis.LogicalConstraint.class;
    }

    @Override
    public @Nullable de.fraunhofer.iais.eis.LogicalConstraint transform(Constraint object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        if (object instanceof MultiplicityConstraint) {
            return transformMultiplicityConstraint((MultiplicityConstraint) object, context);
        } else {
            context.reportProblem(String.format("Cannot transform %s. Supported type: AtomicConstraint", object.getClass().getName()));
            return null;
        }

    }

    @Nullable
    private de.fraunhofer.iais.eis.LogicalConstraint transformMultiplicityConstraint(MultiplicityConstraint constraint, @NotNull TransformerContext context) {
        if (!(constraint instanceof AndConstraint) && !(constraint instanceof OrConstraint) && !(constraint instanceof XoneConstraint)) {
            context.reportProblem("Unsupported multiplicity constraint type: " + constraint.getClass().getName());
            return null;
        }

        var id = IdsId.Builder.newInstance().value(constraint.hashCode()).type(IdsType.CONSTRAINT).build().toUri();

        var builder = new LogicalConstraintBuilder(id);
        for (var edcConstraint : constraint.getConstraints()) {
            if (!(edcConstraint instanceof AtomicConstraint)) {
                context.reportProblem("IDS currently does not support specifying multi-level multiplicity constraints");
                return null;
            }

            var result = context.transform(edcConstraint, de.fraunhofer.iais.eis.Constraint.class);
            if (constraint instanceof AndConstraint) {
                builder._and_(result);
            } else if (constraint instanceof OrConstraint) {
                builder._or_(result);
            } else {
                builder._xone_(result);
            }
        }

        return builder.build();
    }

}
