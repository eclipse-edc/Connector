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

package org.eclipse.dataspaceconnector.azure.cosmos.dialect;

import com.azure.cosmos.models.SqlParameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.azure.cosmos.CosmosDocument.sanitize;

class CosmosConditionExpression {
    private static final String IN_OPERATOR = "in";
    private static final String EQUALS_OPERATOR = "=";
    private static final List<String> SUPPORTED_PREPARED_STATEMENT_OPERATORS = List.of(EQUALS_OPERATOR, IN_OPERATOR);
    private static final String PREPARED_STATEMENT_PLACEHOLDER = "@";
    private final Criterion criterion;
    private final String objectPrefix;

    CosmosConditionExpression(Criterion criterion) {
        this(criterion, null);
    }

    CosmosConditionExpression(Criterion criterion, String objectPrefix) {
        this.criterion = parse(criterion);
        this.objectPrefix = objectPrefix;
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

        Object operandRight = criterion.getOperandRight();
        if (IN_OPERATOR.equalsIgnoreCase(criterion.getOperator()) && !(operandRight instanceof Iterable)) {
            return Result.failure(format("The \"%s\" operator requires the right-hand operand to be of type %s but was actually %s", IN_OPERATOR, Iterable.class, operandRight.getClass()));
        }
        return Result.success();
    }

    /**
     * Converts the {@link Criterion#getOperandRight()} to a {@link List} of {@link SqlParameter}
     * which can then be used to execute prepared statements against CosmosDB.
     */
    public List<SqlParameter> getParameters() {

        var operandRight = criterion.getOperandRight();

        if (operandRight instanceof Iterable) {
            var iterable = (Iterable) operandRight;
            var placeHolders = getPlaceholderValues();

            var iterator = iterable.iterator();
            return placeHolders.stream().map(ph -> new SqlParameter(ph, iterator.next())).collect(Collectors.toList());
        } else {
            var name = getName();
            return List.of(new SqlParameter(generateParameter(name), criterion.getOperandRight()));
        }
    }

    /**
     * Converts the {@link Criterion} into a string representation, that uses statement placeholders ("@xyz").
     * The corresponding parameters are available using {@link CosmosConditionExpression#getParameters()}.
     */
    public String toExpressionString() {
        var operandLeft = sanitize(criterion.getOperandLeft().toString());
        return objectPrefix != null ?
                String.format(" %s.%s %s %s", objectPrefix, operandLeft, criterion.getOperator(), toValuePlaceholder()) :
                String.format(" %s %s %s", operandLeft, criterion.getOperator(), toValuePlaceholder());
    }

    private Criterion parse(Criterion criterion) {
        if (IN_OPERATOR.equalsIgnoreCase(criterion.getOperator())) {
            var tr = new TypeReference<List<String>>() {
            };
            try {
                var list = new ObjectMapper().readValue(criterion.getOperandRight().toString(), tr);
                return new Criterion(criterion.getOperandLeft(), criterion.getOperator(), list);
            } catch (JsonProcessingException e) {
                // not a list
            }
        }
        return criterion;
    }

    /**
     * Converts a right-operand into a simple Cosmos SQL statement placeholder ("@example"), or, if the operand is actually a list, converts
     * it into "(@val1, @val2,...)", with as many placeholders as there are list items.
     * The resulting String does not include the left-operand or the operator.
     */
    private String toValuePlaceholder() {
        var name = getName();
        var operandRight = criterion.getOperandRight();
        if (operandRight instanceof Iterable) {
            return "(" + String.join(", ", getPlaceholderValues()) + ")";
        }
        return PREPARED_STATEMENT_PLACEHOLDER + name;
    }

    private String getName() {
        return criterion.getOperandLeft().toString()
                .replace(":", "_")
                .replace(".", "_");
    }

    /**
     * Converts the right-operand into a set of SQL parameter placeholders, e.g. converts {@code foo IN ["bar", "baz"]} into a List containing
     * {@code ["@foo0", "@foo1"]}.
     * If the right-operand is not a list-type object, it would simply return a singleton list
     */
    private List<String> getPlaceholderValues() {
        var operandRight = criterion.getOperandRight();
        var name = getName();
        if (operandRight instanceof Iterable) {
            var size = (int) StreamSupport.stream(((Iterable) operandRight).spliterator(), false).count();
            return IntStream.range(0, size)
                    .mapToObj(i -> format("%s%s%s", PREPARED_STATEMENT_PLACEHOLDER, name, i))
                    .collect(Collectors.toList());
        } else {
            return Collections.singletonList(generateParameter(name));
        }
    }

    @NotNull
    private String generateParameter(String name) {
        return PREPARED_STATEMENT_PLACEHOLDER + name;
    }
}
