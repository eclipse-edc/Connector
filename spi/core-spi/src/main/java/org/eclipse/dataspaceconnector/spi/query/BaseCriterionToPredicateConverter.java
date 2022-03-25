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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.dataspaceconnector.spi.query;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Converts a {@link Criterion} into a {@link Predicate} of any given type.
 * At this time only "=", "in" and "like" operators are supported.
 *
 * @param <T> The type of object that the Predicate is created for.
 */
public abstract class BaseCriterionToPredicateConverter<T> implements CriterionConverter<Predicate<T>> {

    @Override
    public Predicate<T> convert(Criterion criterion) {
        var operator = criterion.getOperator().toLowerCase();

        switch (operator) {
            case "=": return equalPredicate(criterion);
            case "in": return inPredicate(criterion);
            default:
                throw new IllegalArgumentException(String.format("Operator [%s] is not supported by this converter!", criterion.getOperator()));
        }
    }

    /**
     * Method to extract an object's field's value
     *
     * @param key    Then name of the field
     * @param object The target object
     * @param <R>    The type of the field's value
     */
    protected abstract <R> R property(String key, Object object);

    @NotNull
    private Predicate<T> equalPredicate(Criterion criterion) {
        return t -> {
            Object property = property((String) criterion.getOperandLeft(), t);
            if (property == null) {
                return false; //property does not exist on t
            }
            return Objects.equals(property, criterion.getOperandRight());
        };
    }

    @NotNull
    private Predicate<T> inPredicate(Criterion criterion) {
        return t -> {
            String property = property((String) criterion.getOperandLeft(), t);
            var items = ((String) criterion.getOperandRight())
                    .replace("(", "")
                    .replace(")", "")
                    .replace(" ", "")
                    .split(",");

            return Arrays.asList(items).contains(property);
        };
    }

}
