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

import java.util.List;

import static org.eclipse.edc.sql.translation.FieldTranslator.toParameters;
import static org.eclipse.edc.sql.translation.FieldTranslator.toValuePlaceholder;

public class PlainColumnFieldTranslator implements FieldTranslator {

    private final String columnName;

    public PlainColumnFieldTranslator(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public String getLeftOperand(List<PathItem> path, Class<?> type) {
        return columnName;
    }

    @Override
    public WhereClause toWhereClause(List<PathItem> path, Criterion criterion, SqlOperator operator) {
        return new WhereClause(
                "%s %s %s".formatted(columnName, operator.representation(), toValuePlaceholder(criterion)),
                toParameters(criterion)
        );
    }
}
