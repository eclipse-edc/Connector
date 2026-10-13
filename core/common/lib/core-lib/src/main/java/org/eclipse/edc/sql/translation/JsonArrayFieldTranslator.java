/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.util.reflection.PathItem;

import java.util.List;

/**
 * Translates a canonical path that points to a JSON array column into a SQL {@code EXISTS} sub query that matches when
 * <em>at least one</em> element of the array satisfies the criterion, e.g. {@code contractOffers.id = 'x'} matches every
 * row that has an offer with that id, regardless of its position in the array.
 * <p>
 * The element predicate is delegated to a {@link JsonFieldTranslator} so that arbitrary nested paths and operators
 * (e.g. {@code contractOffers.policy.assignee}) are supported.
 * <p>
 * Not to be confused with {@link JsonArrayTranslator}, which matches a scalar value against the array as a whole using
 * the {@code ??} (contains) operator only; this translator matches a field <em>within</em> each element with any operator.
 */
public class JsonArrayFieldTranslator implements FieldTranslator {

    private static final String ELEMENT_ALIAS = "element";
    private static final String ELEMENT_COLUMN = ELEMENT_ALIAS + ".value";

    private final String columnName;
    private final JsonFieldTranslator elementTranslator;

    public JsonArrayFieldTranslator(String columnName) {
        this.columnName = columnName;
        this.elementTranslator = new JsonFieldTranslator(ELEMENT_COLUMN);
    }

    @Override
    public String getLeftOperand(List<PathItem> path, Class<?> rightOperandType) {
        return elementTranslator.getLeftOperand(path, rightOperandType);
    }

    @Override
    public WhereClause toWhereClause(List<PathItem> path, Criterion criterion, SqlOperator operator) {
        var elementClause = elementTranslator.toWhereClause(path, criterion, operator);
        var sql = "EXISTS (SELECT 1 FROM json_array_elements(%s) AS %s WHERE %s)"
                .formatted(columnName, ELEMENT_ALIAS, elementClause.sql());
        return new WhereClause(sql, elementClause.parameters());
    }
}
