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

import de.fraunhofer.iais.eis.Action;
import de.fraunhofer.iais.eis.ProhibitionBuilder;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public class ProhibitionToIdsProhibitionTransformer implements IdsTypeTransformer<Prohibition, de.fraunhofer.iais.eis.Prohibition> {

    @Override
    public Class<Prohibition> getInputType() {
        return Prohibition.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.Prohibition> getOutputType() {
        return de.fraunhofer.iais.eis.Prohibition.class;
    }

    @Override
    public @Nullable de.fraunhofer.iais.eis.Prohibition transform(@NotNull Prohibition object, @NotNull TransformerContext context) {
        var id = IdsId.Builder.newInstance().value(object.hashCode()).type(IdsType.PROHIBITION).build().toUri();
        var builder = new ProhibitionBuilder(id);

        var action = context.transform(object.getAction(), Action.class);
        if (action != null) {
            builder._action_(action);
        }

        var assigner = object.getAssigner();
        if (assigner != null) {
            builder._assigner_(URI.create(assigner));
        }

        var assignee = object.getAssignee();
        if (assignee != null) {
            builder._assignee_(URI.create(assignee));
        }

        var target = object.getTarget();
        if (target != null) {
            builder._target_(URI.create(target));
        }

        for (var edcConstraint : object.getConstraints()) {
            if (edcConstraint instanceof MultiplicityConstraint) {
                builder._constraint_(context.transform(edcConstraint, de.fraunhofer.iais.eis.LogicalConstraint.class));
            } else {
                builder._constraint_(context.transform(edcConstraint, de.fraunhofer.iais.eis.Constraint.class));
            }
        }

        try {
            return builder.build();
        } catch (ConstraintViolationException e) {
            context.reportProblem(String.format("Failed to build IDS prohibition: %s", e.getMessage()));
            return null;
        }
    }
}
