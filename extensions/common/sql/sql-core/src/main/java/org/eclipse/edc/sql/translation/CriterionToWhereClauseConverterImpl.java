/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import org.eclipse.edc.spi.result.Result;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.Collections.unmodifiableCollection;

public class CriterionToWhereClauseConverterImpl implements CriterionToWhereClauseConverter {

    private static final String IN_OPERATOR = "in";
    private static final String LIKE_OPERATOR = "like";
    private static final String EQUALS_OPERATOR = "=";
    private static final List<String> SUPPORTED_PREPARED_STATEMENT_OPERATORS = List.of(EQUALS_OPERATOR, LIKE_OPERATOR, IN_OPERATOR);
    private static final String PREPARED_STATEMENT_PLACEHOLDER = "?";

    private final TranslationMapping translationMapping;
    private final boolean validateOperator;

    public CriterionToWhereClauseConverterImpl(TranslationMapping translationMapping, boolean validateOperator) {
        this.translationMapping = translationMapping;
        this.validateOperator = validateOperator;
    }

    @Override
    public WhereClause convert(Criterion criterion) {
        var rightOperandClass = Optional.ofNullable(criterion.getOperandRight()).map(Object::getClass).orElse(null);
        var newCriterion = Optional.ofNullable(criterion.getOperandLeft())
                .map(Object::toString)
                .map(it -> translationMapping.getStatement(it, rightOperandClass))
                .map(criterion::withLeftOperand)
                .orElseGet(() -> Criterion.criterion("0", "=", 1));

        if (validateOperator) {
            isValidExpression(criterion)
                    .orElseThrow(f -> new IllegalArgumentException("This expression is not valid: " + f.getFailureDetail()));
        }

        var sql = format("%s %s %s", newCriterion.getOperandLeft(), newCriterion.getOperator(), toValuePlaceholder(newCriterion));
        return new WhereClause(sql, toParameters(newCriterion));
    }

    public Result<Void> isValidExpression(Criterion criterion) {
        var isSupportedOperator = SUPPORTED_PREPARED_STATEMENT_OPERATORS.contains(criterion.getOperator().toLowerCase());
        if (!isSupportedOperator) {
            return Result.failure("unsupported operator " + criterion.getOperator());
        }

        if (Objects.equals(IN_OPERATOR, criterion.getOperator()) && !(criterion.getOperandRight() instanceof Iterable)) {
            return Result.failure(format("The \"%s\" operator requires the right-hand operand to be of type %s", IN_OPERATOR, Iterable.class));
        }
        return Result.success();
    }

    public String toValuePlaceholder(Criterion criterion) {
        if (criterion.getOperandRight() instanceof Collection<?> collection) {
            return format("(%s)", String.join(",", nCopies(collection.size(), PREPARED_STATEMENT_PLACEHOLDER)));
        }
        return PREPARED_STATEMENT_PLACEHOLDER;
    }

    public Collection<Object> toParameters(Criterion criterion) {
        var operandRight = criterion.getOperandRight();
        if (operandRight == null) {
            return emptyList();
        } else if (operandRight instanceof Collection<?> collection) {
            return unmodifiableCollection(collection);
        } else {
            return List.of(operandRight);
        }
    }

}
