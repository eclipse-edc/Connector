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

package org.eclipse.edc.connector.core.store;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.CriterionToPredicateConverter;
import org.eclipse.edc.util.reflection.ReflectionException;
import org.eclipse.edc.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Concrete implementation
 */
public class CriterionToPredicateConverterImpl implements CriterionToPredicateConverter {

    @Override
    public <T> Predicate<T> convert(Criterion criterion) {
        var operator = criterion.getOperator().toLowerCase();

        return switch (operator) {
            case "=" -> equalPredicate(criterion);
            case "in" -> inPredicate(criterion);
            case "like" -> likePredicate(criterion);
            case "contains" -> containsPredicate(criterion);
            default ->
                    throw new IllegalArgumentException(format("Operator [%s] is not supported by this converter!", criterion.getOperator()));
        };
    }

    protected Object property(String key, Object object) {
        try {
            return ReflectionUtil.getFieldValue(key, object);
        } catch (ReflectionException e) {
            return null;
        }
    }

    private <T> Predicate<T> containsPredicate(Criterion criterion) {
        return t -> {
            var operandLeft = (String) criterion.getOperandLeft();
            var operandRight = criterion.getOperandRight();
            var property = property(operandLeft, t);
            if (property == null) {
                return false;
            }

            if (property instanceof Collection<?> collection) {
                return collection.contains(operandRight);
            }

            return false;
        };
    }

    @NotNull
    private <T> Predicate<T> equalPredicate(Criterion criterion) {
        return t -> {
            var operandLeft = (String) criterion.getOperandLeft();
            var property = property(operandLeft, t);
            if (property == null) {
                return false;
            }

            if (property.getClass().isEnum() && criterion.getOperandRight() instanceof String) {
                var enumProperty = (Enum<?>) property;
                return Objects.equals(enumProperty.name(), criterion.getOperandRight());
            }

            if (property instanceof Number c1 && criterion.getOperandRight() instanceof Number c2) {
                // interpret as double to not lose any precision
                return Double.compare(c1.doubleValue(), c2.doubleValue()) == 0;
            }

            if (property instanceof List<?> list) {
                return list.stream().anyMatch(it -> Objects.equals(it, criterion.getOperandRight()));
            }

            return Objects.equals(property, criterion.getOperandRight());
        };
    }

    @NotNull
    private <T> Predicate<T> inPredicate(Criterion criterion) {
        return t -> {
            var operandLeft = (String) criterion.getOperandLeft();
            var property = property(operandLeft, t);
            if (property == null) {
                return false;
            }

            if (criterion.getOperandRight() instanceof Iterable<?> iterable) {
                for (var value : iterable) {
                    if (value.equals(property)) {
                        return true;
                    }
                }
                return false;
            } else {
                throw new IllegalArgumentException("Operator IN requires the right-hand operand to be an " + Iterable.class.getName() + " but was " + criterion.getOperandRight().getClass().getName());
            }


        };
    }

    @NotNull
    private <T> Predicate<T> likePredicate(Criterion criterion) {
        return t -> {
            var operandLeft = (String) criterion.getOperandLeft();
            var property = property(operandLeft, t);
            if (property == null) {
                return false;
            }

            if (criterion.getOperandRight() instanceof String operandRight) {
                var regexPattern = Pattern.quote(operandRight)
                        .replace("%", "\\E.*\\Q")
                        .replace("_", "\\E.\\Q");

                regexPattern = "^" + regexPattern + "$";

                return Pattern.compile(regexPattern).matcher(property.toString()).matches();
            }

            return false;
        };
    }

}
