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
 *       Fraunhofer Institute for Software and Systems Engineering - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.DutyBuilder;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.MultiplicityConstraint;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
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

        IdsId idsId = IdsId.Builder.newInstance().value(object.hashCode()).type(IdsType.PERMISSION).build();
        URI id = context.transform(idsId, URI.class);
        DutyBuilder dutyBuilder = new DutyBuilder(id);

        for (org.eclipse.dataspaceconnector.policy.model.Constraint edcConstraint : object.getConstraints()) {
            de.fraunhofer.iais.eis.AbstractConstraint idsConstraint;
            if (edcConstraint instanceof MultiplicityConstraint) {
                idsConstraint = context.transform(edcConstraint, de.fraunhofer.iais.eis.LogicalConstraint.class);
            } else {
                idsConstraint = context.transform(edcConstraint, de.fraunhofer.iais.eis.Constraint.class);
            }
            dutyBuilder._constraint_(idsConstraint);
        }

        String target = object.getTarget();
        if (target != null) {
            dutyBuilder._target_(URI.create(target));
        }

        String assigner = object.getAssigner();
        if (assigner != null) {
            dutyBuilder._assigner_(URI.create(assigner));
        }

        String assignee = object.getAssignee();
        if (assignee != null) {
            dutyBuilder._assignee_(URI.create(assignee));
        }

        de.fraunhofer.iais.eis.Action action = context.transform(object.getAction(), de.fraunhofer.iais.eis.Action.class);
        dutyBuilder._action_(action);

        de.fraunhofer.iais.eis.Duty duty;
        try {
            duty = dutyBuilder.build();
        } catch (ConstraintViolationException e) {
            context.reportProblem(String.format("Failed to build IDS duty: %s", e.getMessage()));
            duty = null;
        }

        return duty;
    }
}
