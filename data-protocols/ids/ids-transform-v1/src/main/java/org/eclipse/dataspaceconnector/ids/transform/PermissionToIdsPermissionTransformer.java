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

import de.fraunhofer.iais.eis.AbstractConstraint;
import de.fraunhofer.iais.eis.Duty;
import de.fraunhofer.iais.eis.PermissionBuilder;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.MultiplicityConstraint;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
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

        IdsId idsId = IdsId.Builder.newInstance().value(object.hashCode()).type(IdsType.PERMISSION).build();
        URI id = context.transform(idsId, URI.class);
        PermissionBuilder permissionBuilder = new PermissionBuilder(id);

        for (Constraint edcConstraint : object.getConstraints()) {
            AbstractConstraint idsConstraint;
            if (edcConstraint instanceof MultiplicityConstraint) {
                idsConstraint = context.transform(edcConstraint, de.fraunhofer.iais.eis.LogicalConstraint.class);
            } else {
                idsConstraint = context.transform(edcConstraint, de.fraunhofer.iais.eis.Constraint.class);
            }
            permissionBuilder._constraint_(idsConstraint);
        }

        var idsArtifactId = IdsId.Builder.newInstance()
                .type(IdsType.ARTIFACT)
                .value(object.getTarget())
                .build();
        var uri = context.transform(idsArtifactId, URI.class);

        if (uri != null) {
            permissionBuilder._target_(uri);
        }

        String assigner = object.getAssigner();
        if (assigner != null) {
            permissionBuilder._assigner_(Collections.singletonList(URI.create(assigner)));
        }

        String assignee = object.getAssignee();
        if (assignee != null) {
            permissionBuilder._assignee_(Collections.singletonList(URI.create(assignee)));
        }

        if (object.getAction() != null) {
            var action = context.transform(object.getAction(), de.fraunhofer.iais.eis.Action.class);
            permissionBuilder._action_(action);
        }

        if (object.getDuties() != null) {
            var duties = new ArrayList<Duty>();
            for (var edcDuty : object.getDuties()) {
                var duty = context.transform(edcDuty, Duty.class);
                duties.add(duty);
            }
            permissionBuilder._preDuty_(duties);
        }

        de.fraunhofer.iais.eis.Permission permission;
        try {
            permission = permissionBuilder.build();
        } catch (ConstraintViolationException e) {
            context.reportProblem(String.format("Failed to build IDS permission: %s", e.getMessage()));
            permission = null;
        }

        return permission;
    }
}
