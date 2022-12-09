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

import de.fraunhofer.iais.eis.Duty;
import de.fraunhofer.iais.eis.PermissionBuilder;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PermissionToIdsPermissionTransformer implements IdsTypeTransformer<Permission, de.fraunhofer.iais.eis.Permission> {

    @Override
    public Class<Permission> getInputType() {
        return Permission.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.Permission> getOutputType() {
        return de.fraunhofer.iais.eis.Permission.class;
    }

    @Override
    public @Nullable de.fraunhofer.iais.eis.Permission transform(Permission object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        var permissionId = IdsId.Builder.newInstance().value(object.hashCode()).type(IdsType.PERMISSION).build().toUri();
        var builder = new PermissionBuilder(permissionId);
        for (var edcConstraint : object.getConstraints()) {
            if (edcConstraint instanceof MultiplicityConstraint) {
                builder._constraint_(context.transform(edcConstraint, de.fraunhofer.iais.eis.LogicalConstraint.class));
            } else {
                builder._constraint_(context.transform(edcConstraint, de.fraunhofer.iais.eis.Constraint.class));
            }
        }

        var artifactId = IdsId.Builder.newInstance().value(object.getTarget()).type(IdsType.ARTIFACT).build().toUri();
        if (artifactId != null) {
            builder._target_(artifactId);
        }

        var assigner = object.getAssigner();
            if (assigner != null) {
            builder._assigner_(List.of(URI.create(assigner)));
        }

        var assignee = object.getAssignee();
        if (assignee != null) {
            builder._assignee_(List.of(URI.create(assignee)));
        }

        if (object.getAction() != null) {
            var action = context.transform(object.getAction(), de.fraunhofer.iais.eis.Action.class);
            builder._action_(action);
        }

        if (object.getDuties() != null) {
            var duties = new ArrayList<Duty>();
            for (var edcDuty : object.getDuties()) {
                var duty = context.transform(edcDuty, Duty.class);
                duties.add(duty);
            }

            builder._preDuty_(duties);
        }

        try {
            return builder.build();
        } catch (ConstraintViolationException e) {
            context.reportProblem(String.format("Failed to build IDS permission: %s", e.getMessage()));
            return null;
        }
    }
}
