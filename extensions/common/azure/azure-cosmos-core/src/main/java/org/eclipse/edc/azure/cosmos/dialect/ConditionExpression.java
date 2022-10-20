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

package org.eclipse.edc.azure.cosmos.dialect;

import com.azure.cosmos.models.SqlParameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

/**
 * Represents an abstraction for Cosmos condition rewrite rules.
 * Its responsibility is to validate and translate from {@link Criterion} to a Cosmos SQL condition expression.
 */
public abstract class ConditionExpression {

    public static final String IN_OPERATOR = "in";
    public static final String EQUALS_OPERATOR = "=";
    public static final String LIKE_OPERATOR = "like";
    public static final List<String> SUPPORTED_PREPARED_STATEMENT_OPERATORS = List.of(EQUALS_OPERATOR, IN_OPERATOR, LIKE_OPERATOR);

    public static final List<String> RESERVED_WORDS = List.of("value");


    public static final String PREPARED_STATEMENT_PLACEHOLDER = "@";


    private final Criterion criterion;

    protected ConditionExpression(Criterion criterion) {
        this.criterion = parse(criterion);
    }


    /**
     * Checks whether the given {@link Criterion} is valid or not, i.e. if its {@linkplain Criterion#getOperator()} is
     * in the list of supported operators, and whether the {@linkplain Criterion#getOperandRight()} has the correct
     * type.
     */
    public Result<Void> isValidExpression() {
        var criterion = getCriterion();
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
     * Returns the criterion
     *
     * @return {@link Criterion}
     */

    public Criterion getCriterion() {
        return criterion;
    }

    /**
     * Returns the criterion
     *
     * @return The field path of the left operand
     */

    public abstract String getFieldPath();

    /**
     * Converts the {@link Criterion} into a string representation, that uses statement placeholders ("@xyz"). The
     * corresponding parameters are available using {@link ConditionExpression#getParameters()}.
     */
    public abstract String toExpressionString();

    /**
     * This method replace the dot notation with the quoted property operator [\"\"] using the {@link QuotedPathCollector}
     *
     * @param path The input path
     * @return the quoted path
     */
    public String quotePath(String path) {
        return Arrays.stream(path.split(Pattern.quote("."))).collect(QuotedPathCollector.quoteJoining(RESERVED_WORDS));
    }

    /**
     * Converts a right-operand into a simple Cosmos SQL statement placeholder ("@example"), or, if the operand is
     * actually a list, converts it into "(@val1, @val2,...)", with as many placeholders as there are list items. The
     * resulting String does not include the left-operand or the operator.
     */
    protected String toValuePlaceholder() {
        var name = getName();
        var operandRight = getCriterion().getOperandRight();
        if (operandRight instanceof Iterable) {
            return "(" + String.join(", ", getPlaceholderValues()) + ")";
        }
        return PREPARED_STATEMENT_PLACEHOLDER + name;
    }

    protected Criterion parse(Criterion criterion) {
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
     * Converts the {@link Criterion#getOperandRight()} to a {@link List} of {@link SqlParameter} which can then be used
     * to execute prepared statements against CosmosDB.
     */
    protected List<SqlParameter> getParameters() {

        var criterion = getCriterion();

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
     * Converts the right-operand into a set of SQL parameter placeholders, e.g. converts {@code foo IN ["bar", "baz"]}
     * into a List containing {@code ["@foo0", "@foo1"]}. If the right-operand is not a list-type object, it would
     * simply return a singleton list
     */
    private List<String> getPlaceholderValues() {
        var criterion = getCriterion();

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

    private String getName() {
        return getFieldPath()
                .replace(":", "_")
                .replace(".", "_")
                .replace("[", "")
                .replace("]", "")
                .replaceAll("[0-9]", "");
    }

    @NotNull
    private String generateParameter(String name) {
        return PREPARED_STATEMENT_PLACEHOLDER + name;
    }

}
