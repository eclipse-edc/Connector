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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.spi.query;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Converts a {@link Criterion} into a {@link Predicate} of any given type. At this time only "=", "in" and "like"
 * operators are supported.
 *
 * @param <T> The type of object that the Predicate is created for.
 */
public abstract class BaseCriterionToPredicateConverter<T> implements CriterionConverter<Predicate<T>> {

    @Override
    public Predicate<T> convert(Criterion criterion) {
        var operator = criterion.getOperator().toLowerCase();

        switch (operator) {
            case "=":
                return equalPredicate(criterion);
            case "in":
                return inPredicate(criterion);
            default:
                throw new IllegalArgumentException(String.format("Operator [%s] is not supported by this converter!", criterion.getOperator()));
        }
    }

    /**
     * Method to extract an object's field's value
     *
     * @param key Then name of the field
     * @param object The target object
     */
    protected abstract Object property(String key, Object object);

    @NotNull
    private Predicate<T> equalPredicate(Criterion criterion) {
        return t -> {
            var property = property((String) criterion.getOperandLeft(), t);
            if (property == null) {
                return false; //property does not exist on t
            }

            if (property.getClass().isEnum() && criterion.getOperandRight() instanceof String) {
                var enumProperty = (Enum<?>) property;
                return Objects.equals(enumProperty.name(), criterion.getOperandRight());
            }

            return Objects.equals(property, criterion.getOperandRight());
        };
    }

    @NotNull
    private Predicate<T> inPredicate(Criterion criterion) {
        return t -> {
            var property = property((String) criterion.getOperandLeft(), t);

            var rightOp = criterion.getOperandRight();

            if (rightOp instanceof Iterable) {
                var iterable = (Iterable<?>) rightOp;
                for (var value : iterable) {
                    if (value.equals(property)) {
                        return true;
                    }
                }
                return false;
            } else {
                throw new IllegalArgumentException("Operator IN requires the right-hand operand to be an " + Iterable.class.getName() + " but was " + rightOp.getClass().getName());
            }


        };
    }

}
