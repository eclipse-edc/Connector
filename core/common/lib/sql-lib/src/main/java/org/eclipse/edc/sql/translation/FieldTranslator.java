/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.util.reflection.PathItem;

import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.Collections.unmodifiableCollection;

/**
 * Component that can translate a canonical field path into a sql column name or path.
 */
public interface FieldTranslator {

    String PREPARED_STATEMENT_PLACEHOLDER = "?";

    /**
     * Get left operand for a SQL query given the canonical path name and the type
     *
     * @param path the path.
     * @return the left operand.
     */
    String getLeftOperand(List<PathItem> path, Class<?> rightOperandType);

    /**
     * Translate the {@link Criterion} into a {@link WhereClause}.
     *
     * @param path the path.
     * @param criterion the criterion.
     * @param operator the sql operator.
     * @return the {@link WhereClause}.
     */
    WhereClause toWhereClause(List<PathItem> path, Criterion criterion, SqlOperator operator);

    default Collection<Object> toParameters(Criterion criterion) {
        var operandRight = criterion.getOperandRight();
        if (operandRight == null) {
            return emptyList();
        } else if (operandRight instanceof Collection<?> collection) {
            return unmodifiableCollection(collection);
        } else {
            return List.of(operandRight);
        }
    }

    static String toValuePlaceholder(Criterion criterion) {
        if (criterion.getOperandRight() instanceof Collection<?> collection) {
            return format("(%s)", String.join(",", nCopies(collection.size(), PREPARED_STATEMENT_PLACEHOLDER)));
        }
        return PREPARED_STATEMENT_PLACEHOLDER;
    }

}
