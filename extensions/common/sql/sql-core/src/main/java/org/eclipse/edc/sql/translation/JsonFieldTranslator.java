/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.util.reflection.PathItem;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.stream.IntStream.range;
import static org.eclipse.edc.sql.translation.FieldTranslator.toParameters;
import static org.eclipse.edc.sql.translation.FieldTranslator.toValuePlaceholder;

public class JsonFieldTranslator implements FieldTranslator {
    protected final String columnName;

    public JsonFieldTranslator(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public String getLeftOperand(List<PathItem> path, Class<?> type) {
        var statementBuilder = new StringBuilder(columnName);

        var length = path.size();
        range(0, length - 1)
                .mapToObj(i -> " -> '%s'".formatted(path.get(i)))
                .forEach(statementBuilder::append);

        statementBuilder.append(" ->> '%s'".formatted(path.get(length - 1)));
        var statement = statementBuilder.toString();

        return checkStatementByType(type, statement);
    }

    @Override
    public WhereClause toWhereClause(List<PathItem> path, Criterion criterion, SqlOperator operator) {
        var leftOperand = getLeftOperand(path, criterion.getOperandRight().getClass());

        var amendedLeftOperand = Optional.of(leftOperand)
                .filter(it -> operator.representation().equals("??"))
                .map(it -> it.replace("->>", "->"))
                .map("(%s)::jsonb"::formatted)
                .orElse(leftOperand);

        return new WhereClause(
                "%s %s %s".formatted(amendedLeftOperand, operator.representation(), toValuePlaceholder(criterion)),
                toParameters(criterion)
        );
    }

    private String checkStatementByType(Class<?> type, String statement) {
        if (type.equals(Boolean.class)) {
            return format("(%s)::boolean", statement);
        }

        if (type.equals(Integer.class)) {
            return format("(%s)::integer", statement);
        }

        if (type.equals(Double.class)) {
            return format("(%s)::double", statement);
        }

        if (type.equals(Float.class)) {
            return format("(%s)::float", statement);
        }

        if (type.equals(Long.class)) {
            return format("(%s)::long", statement);
        }

        if (type.equals(Byte.class)) {
            return format("(%s)::byte", statement);
        }

        if (type.equals(Short.class)) {
            return format("(%s)::short", statement);
        }

        return statement;
    }
}
