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
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.transform.type.policy;

import de.fraunhofer.iais.eis.DutyBuilder;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

public class DutyToIdsDutyTransformer implements IdsTypeTransformer<Duty, de.fraunhofer.iais.eis.Duty> {

    @Override
    public Class<Duty> getInputType() {
        return Duty.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.Duty> getOutputType() {
        return de.fraunhofer.iais.eis.Duty.class;
    }

    @Override
    public @Nullable de.fraunhofer.iais.eis.Duty transform(Duty object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        var id = IdsId.Builder.newInstance().value(object.hashCode()).type(IdsType.OBLIGATION).build().toUri();
        var builder = new DutyBuilder(id);
        for (var edcConstraint : object.getConstraints()) {
            if (edcConstraint instanceof MultiplicityConstraint) {
                builder._constraint_(context.transform(edcConstraint, de.fraunhofer.iais.eis.LogicalConstraint.class));
            } else {
                builder._constraint_(context.transform(edcConstraint, de.fraunhofer.iais.eis.Constraint.class));
            }
        }

        var target = object.getTarget();
        if (target != null) {
            builder._target_(URI.create(target));
        }

        var assigner = object.getAssigner();
        if (assigner != null) {
            builder._assigner_(URI.create(assigner));
        }

        var assignee = object.getAssignee();
        if (assignee != null) {
            builder._assignee_(URI.create(assignee));
        }

        var action = context.transform(object.getAction(), de.fraunhofer.iais.eis.Action.class);
        builder._action_(action);

        try {
            return builder.build();
        } catch (ConstraintViolationException e) {
            context.reportProblem(String.format("Failed to build IDS duty: %s", e.getMessage()));
            return null;
        }
    }
}
