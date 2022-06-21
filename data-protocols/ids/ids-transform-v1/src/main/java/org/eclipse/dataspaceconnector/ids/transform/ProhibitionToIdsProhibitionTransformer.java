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
import de.fraunhofer.iais.eis.Action;
import de.fraunhofer.iais.eis.ProhibitionBuilder;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.policy.model.MultiplicityConstraint;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

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
    public @Nullable de.fraunhofer.iais.eis.Prohibition transform(Prohibition object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        IdsId idsId = IdsId.Builder.newInstance().value(object.hashCode()).type(IdsType.PROHIBITION).build();
        URI id = context.transform(idsId, URI.class);
        ProhibitionBuilder prohibitionBuilder = new ProhibitionBuilder(id);

        Action action = context.transform(object.getAction(), Action.class);
        if (action != null) {
            prohibitionBuilder._action_(action);
        }

        String assigner = object.getAssigner();
        if (assigner != null) {
            prohibitionBuilder._assigner_(URI.create(assigner));
        }

        String assignee = object.getAssignee();
        if (assignee != null) {
            prohibitionBuilder._assignee_(URI.create(assignee));
        }

        String target = object.getTarget();
        if (target != null) {
            prohibitionBuilder._target_(URI.create(target));
        }

        for (org.eclipse.dataspaceconnector.policy.model.Constraint edcConstraint : object.getConstraints()) {
            AbstractConstraint idsConstraint;
            if (edcConstraint instanceof MultiplicityConstraint) {
                idsConstraint = context.transform(edcConstraint, de.fraunhofer.iais.eis.LogicalConstraint.class);
            } else {
                idsConstraint = context.transform(edcConstraint, de.fraunhofer.iais.eis.Constraint.class);
            }

            prohibitionBuilder._constraint_(idsConstraint);
        }

        de.fraunhofer.iais.eis.Prohibition prohibition;
        try {
            prohibition = prohibitionBuilder.build();
        } catch (ConstraintViolationException e) {
            context.reportProblem(String.format("Failed to build IDS prohibition: %s", e.getMessage()));
            prohibition = null;
        }

        return prohibition;
    }
}
