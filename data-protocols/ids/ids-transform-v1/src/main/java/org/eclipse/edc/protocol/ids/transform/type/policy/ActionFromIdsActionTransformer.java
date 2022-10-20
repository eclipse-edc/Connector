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

import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ActionFromIdsActionTransformer implements IdsTypeTransformer<de.fraunhofer.iais.eis.Action, Action> {

    @Override
    public Class<de.fraunhofer.iais.eis.Action> getInputType() {
        return de.fraunhofer.iais.eis.Action.class;
    }

    @Override
    public Class<Action> getOutputType() {
        return Action.class;
    }

    @Override
    public @Nullable Action transform(de.fraunhofer.iais.eis.Action object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        return Action.Builder.newInstance().type(object.name()).build();
    }
}
