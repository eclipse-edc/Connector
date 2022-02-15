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

package org.eclipse.dataspaceconnector.spi.query;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Converts a {@link Criterion} into a {@link Predicate} of any given type.
 * At this time only "=" and "in" operators are supported.
 *
 * @param <T> The type of object that the Predicate is created for.
 */
public abstract class BaseCriterionToPredicateConverter<T> implements CriterionConverter<Predicate<T>> {
    @Override
    public Predicate<T> convert(Criterion criterion) {
        if ("=".equals(criterion.getOperator())) {
            return t -> {
                Object property = property((String) criterion.getOperandLeft(), t);
                if (property == null) {
                    return false; //property does not exist on t
                }
                return Objects.equals(property, criterion.getOperandRight());
            };
        } else if ("in".equalsIgnoreCase(criterion.getOperator())) {
            return t -> {
                String property = property((String) criterion.getOperandLeft(), t);
                var list = (String) criterion.getOperandRight();
                // some cleanup needs to happen
                list = list.replace("(", "").replace(")", "").replace(" ", "");
                var items = list.split(",");
                return Arrays.asList(items).contains(property);
            };
        }
        throw new IllegalArgumentException(String.format("Operator [%s] is not supported by this converter!", criterion.getOperator()));
    }

    /**
     * Method to extract an object's field's value
     *
     * @param key    Then name of the field
     * @param object The target object
     * @param <R>    The type of the field's value
     */
    protected abstract <R> R property(String key, Object object);
}
