/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.sql.asset.index;

import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

/**
 * Parses a {@link Criterion} and provides methods to validate it and extract SQL prepared statement placeholders and parameters.
 */
class SqlConditionExpression {

    private static final String IN_OPERATOR = "in";
    private static final String LIKE_OPERATOR = "like";
    private static final String EQUALS_OPERATOR = "=";
    private static final List<String> SUPPORTED_PREPARED_STATEMENT_OPERATORS = List.of(EQUALS_OPERATOR, LIKE_OPERATOR, IN_OPERATOR);
    private static final String PREPARED_STATEMENT_PLACEHOLDER = "?";
    private final Criterion criterion;

    public SqlConditionExpression(Criterion criterion) {

        this.criterion = criterion;
    }

    /**
     * Checks whether the given {@link Criterion} is valid or not, i.e. if its {@linkplain Criterion#getOperator()} is in the list
     * of supported operators, and whether the {@linkplain Criterion#getOperandRight()} has the correct type.
     */
    public Result<Void> isValidExpression() {
        var isSupportedOperator = SUPPORTED_PREPARED_STATEMENT_OPERATORS.contains(criterion.getOperator().toLowerCase());
        if (!isSupportedOperator) {
            return Result.failure("unsupported operator " + criterion.getOperator());
        }

        if (Objects.equals(IN_OPERATOR, criterion.getOperator()) && !(criterion.getOperandRight() instanceof Iterable)) {
            return Result.failure(format("The \"%s\" operator requires the right-hand operand to be of type %s", IN_OPERATOR, Iterable.class));
        }
        return Result.success();
    }


    /**
     * Converts an operand into a simple SQL statement placeholder ("?"), or, if the operand is actually a list, converts
     * it into "(?,...?)", with as many placeholders as there are list items
     */
    public String toValuePlaceholder() {
        var operandRight = criterion.getOperandRight();
        if (operandRight instanceof Iterable) {
            var size = StreamSupport.stream(((Iterable) operandRight).spliterator(), false).count();
            return format("(%s)", String.join(",", Collections.nCopies((int) size, PREPARED_STATEMENT_PLACEHOLDER)));
        }
        return PREPARED_STATEMENT_PLACEHOLDER;
    }

    /**
     * Converts the {@link Criterion#getOperandRight()} to a {@link Stream} of Strings
     * which can then be used as parameters for  prepared statements.
     * <p>
     * Note: {@link Stream} is used to allow a convenient use in a {@code flatMap} call
     */
    @SuppressWarnings("unchecked")
    public Stream<String> toStatementParameter() {
        List<String> result = new ArrayList<>();
        result.add(criterion.getOperandLeft().toString());

        var operandRight = criterion.getOperandRight();
        if (operandRight instanceof Iterable) {
            var iterable = (Iterable) operandRight;
            iterable.forEach(o -> result.add(o.toString()));
        } else {
            result.add(operandRight.toString());
        }
        return result.stream();
    }

    public Criterion getCriterion() {
        return criterion;
    }
}
