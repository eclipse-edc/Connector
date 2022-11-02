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

import de.fraunhofer.iais.eis.BinaryOperator;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class OperatorToIdsBinaryOperatorTransformer implements IdsTypeTransformer<Operator, BinaryOperator> {
    private static final Map<Operator, BinaryOperator> MAPPING = new HashMap<>() {
        {
            put(Operator.EQ, BinaryOperator.EQUALS);
            put(Operator.GT, BinaryOperator.GT);
            put(Operator.GEQ, BinaryOperator.GTEQ);
            put(Operator.LT, BinaryOperator.LT);
            put(Operator.LEQ, BinaryOperator.LTEQ);
            put(Operator.IN, BinaryOperator.IN);
        }
    };

    @Override
    public Class<Operator> getInputType() {
        return Operator.class;
    }

    @Override
    public Class<BinaryOperator> getOutputType() {
        return BinaryOperator.class;
    }

    @Override
    public @Nullable BinaryOperator transform(Operator object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        var binaryOperator = MAPPING.get(object);
        if (binaryOperator == null) {
            context.reportProblem(String.format("Can not transform %s to IDS BinaryOperator", object.name()));
            return null;
        }

        return binaryOperator;
    }
}
