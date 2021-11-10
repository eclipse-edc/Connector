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

import de.fraunhofer.iais.eis.Constraint;
import de.fraunhofer.iais.eis.DutyBuilder;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class DutyToDutyTransformer implements IdsTypeTransformer<Duty, de.fraunhofer.iais.eis.Duty> {

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

        var idsId = IdsId.Builder.newInstance().value(object.hashCode()).type(IdsType.PERMISSION).build();
        var id = context.transform(idsId, URI.class);
        var dutyBuilder = new DutyBuilder(id);

        var idsConstraints = new ArrayList<Constraint>();
        for (var edcConstraint : object.getConstraints()) {
            var idsConstraint = context.transform(edcConstraint, de.fraunhofer.iais.eis.Constraint.class);
            idsConstraints.add(idsConstraint);
        }
        dutyBuilder._constraint_(idsConstraints);

        var target = object.getTarget();
        if (target != null) {
            dutyBuilder._target_(URI.create(target));
        }

        var assigner = object.getAssigner();
        if (assigner != null) {
            dutyBuilder._assigner_(new ArrayList<>(Collections.singletonList(URI.create(assigner))));
        }
        var assignee = object.getAssignee();
        if (assignee != null) {
            dutyBuilder._assignee_(new ArrayList<>(Collections.singletonList(URI.create(assignee))));
        }

        var action = context.transform(object.getAction(), de.fraunhofer.iais.eis.Action.class);
        dutyBuilder._action_(new ArrayList<>(Collections.singletonList(action)));

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
