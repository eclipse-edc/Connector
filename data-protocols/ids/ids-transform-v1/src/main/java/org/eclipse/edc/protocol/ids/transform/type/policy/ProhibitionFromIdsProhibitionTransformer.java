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

package org.eclipse.edc.protocol.ids.transform.type.policy;

import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ProhibitionFromIdsProhibitionTransformer implements IdsTypeTransformer<de.fraunhofer.iais.eis.Prohibition, Prohibition> {

    @Override
    public Class<de.fraunhofer.iais.eis.Prohibition> getInputType() {
        return de.fraunhofer.iais.eis.Prohibition.class;
    }

    @Override
    public Class<Prohibition> getOutputType() {
        return Prohibition.class;
    }

    @Override
    public @Nullable Prohibition transform(de.fraunhofer.iais.eis.Prohibition object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        var builder = Prohibition.Builder.newInstance();
        for (var idsConstraint : object.getConstraint()) {
            var edcConstraint = context.transform(idsConstraint, Constraint.class);
            builder.constraint(edcConstraint);
        }

        if (object.getAction() != null && object.getAction().size() != 0) {
            if (object.getAction().size() > 1) {
                context.reportProblem("Cannot map multiple IDS prohibition actions to EDC (ODRL)");
            }
            builder.action(Action.Builder.newInstance().type(object.getAction().get(0).name()).build());
        }

        if (object.getAssigner() != null && object.getAssigner().size() != 0) {
            if (object.getAssigner().size() > 1) {
                context.reportProblem("Cannot map multiple IDS prohibition assigner to EDC (ODRL)");
            }

            builder.assigner(object.getAssigner().get(0).toString());
        }

        if (object.getAssignee() != null && object.getAssignee().size() != 0) {
            if (object.getAssignee().size() > 1) {
                context.reportProblem("Cannot map multiple IDS prohibition assignee to EDC (ODRL)");
            }

            builder.assignee(object.getAssignee().get(0).toString());
        }

        if (object.getTarget() != null) {
            builder.target(object.getTarget().toString());
        }

        return builder.build();
    }
}
