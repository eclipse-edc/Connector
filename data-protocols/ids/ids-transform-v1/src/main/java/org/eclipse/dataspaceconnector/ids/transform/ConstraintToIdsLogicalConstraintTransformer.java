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
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.LogicalConstraintBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.policy.model.AndConstraint;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.MultiplicityConstraint;
import org.eclipse.dataspaceconnector.policy.model.OrConstraint;
import org.eclipse.dataspaceconnector.policy.model.XoneConstraint;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
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
            context.reportProblem(String.format("Cannot transform %s. Supported Constraints: '%s'", object.getClass().getName(), AtomicConstraint.class.getName()));
            return null;
        }

    }

    @Nullable
    private de.fraunhofer.iais.eis.LogicalConstraint transformMultiplicityConstraint(MultiplicityConstraint constraint, @NotNull TransformerContext context) {
        if (!(constraint instanceof AndConstraint) && !(constraint instanceof OrConstraint) && !(constraint instanceof XoneConstraint)) {
            context.reportProblem("Unsupported multiplicity contraint type: " + constraint.getClass().getName());
            return null;
        }
        var idsId = IdsId.Builder.newInstance().value(constraint.hashCode()).type(IdsType.CONSTRAINT).build();
        var id = context.transform(idsId, URI.class);

        LogicalConstraintBuilder constraintBuilder = new LogicalConstraintBuilder(id);

        for (Constraint containedConstraint : constraint.getConstraints()) {
            if (!(containedConstraint instanceof AtomicConstraint)) {
                context.reportProblem("IDS currently does not support specifying multi-level multiplicity constraints");
                return null;
            }
            var transformed = context.transform(containedConstraint, de.fraunhofer.iais.eis.Constraint.class);
            if (constraint instanceof AndConstraint) {
                constraintBuilder._and_(transformed);
            } else if (constraint instanceof OrConstraint) {
                constraintBuilder._or_(transformed);
            } else {
                constraintBuilder._xone_(transformed);
            }
        }

        return constraintBuilder.build();
    }

}
