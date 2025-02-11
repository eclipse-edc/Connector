/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.util.reflection.PathItem;

import java.util.Collection;
import java.util.List;

import static org.eclipse.edc.sql.translation.FieldTranslator.toParameters;
import static org.eclipse.edc.sql.translation.FieldTranslator.toValuePlaceholder;

/**
 * This is a specialized translator, that targets object properties that are JSON array, for example {@code someObject.jsonArray},
 * and a criterion of "jsonArray contains foobar".
 * <p>
 * This is necessary, because with a conventional {@link JsonFieldTranslator} we would get {@code (jsonArray -> 'jsonArray')::jsonb ?? ?},
 * because it always assumes one indirection from columnName to object name.
 * </p>
 * The {@link JsonArrayTranslator} explicitly ignores this indirection.
 */
public class JsonArrayTranslator implements FieldTranslator {

    @Override
    public String getLeftOperand(List<PathItem> path, Class<?> rightOperandType) {
        if (Collection.class.isAssignableFrom(rightOperandType)) {
            throw new IllegalArgumentException("JsonArrayTranslator only supports scalar right-operands, found '%s' ".formatted(rightOperandType));
        }
        if (path.size() == 1) {
            return path.get(0).toString();
        }
        throw new IllegalArgumentException("Invalid path for JsonArrayTranslator: must have one element, but found '%s'".formatted(path.size()));
    }

    @Override
    public WhereClause toWhereClause(List<PathItem> path, Criterion criterion, SqlOperator operator) {
        var leftOperand = getLeftOperand(path, criterion.getOperandRight().getClass());

        if (!operator.representation().equals("??")) {
            throw new IllegalArgumentException("Invalid operator for JsonArrayTranslator: must be '??', but was '%s'".formatted(operator.representation()));
        }

        return new WhereClause("%s::jsonb ?? %s".formatted(leftOperand, toValuePlaceholder(criterion)), toParameters(criterion));
    }
}
