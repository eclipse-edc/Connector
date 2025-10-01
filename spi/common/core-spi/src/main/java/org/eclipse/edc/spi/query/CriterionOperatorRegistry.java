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

package org.eclipse.edc.spi.query;

import java.util.function.Predicate;

/**
 * Keep track of the supported {@link Criterion} operators.
 */
public interface CriterionOperatorRegistry {

    String EQUAL = "=";
    String NOT_EQUAL = "!=";
    String IN = "in";
    String NOT_IN = "not in";
    String LIKE = "like";
    String ILIKE = "ilike";
    String CONTAINS = "contains";
    String LESS_THAN = "<";
    String LESS_THAN_EQUAL = "<=";
    String GREATER_THAN = ">";
    String GREATER_THAN_EQUAL = ">=";

    /**
     * Register an operator with the related operator predicate.
     *
     * @param operator  the operator, case-insensitive.
     * @param predicate the operator predicate.
     */
    void registerOperatorPredicate(String operator, OperatorPredicate predicate);

    /**
     * Register a {@link PropertyLookup} instance, that will be called as first one to extract property from an object.
     *
     * @param propertyLookup the property lookup instance.
     */
    void registerPropertyLookup(PropertyLookup propertyLookup);

    /**
     * Unregister an operator.
     *
     * @param operator the operator, case-insensitive.
     */
    void unregister(String operator);

    /**
     * Convert a {@link Criterion} into a {@link Predicate}
     *
     * @param <T> The type of object which the store requires to perform its query.
     * @throws IllegalArgumentException if the criterion cannot be converted.
     */
    <T> Predicate<T> toPredicate(Criterion criterion);

    /**
     * Tell if the operator is supported.
     *
     * @param operator the operator, case-insensitive.
     * @return true if the operator is supported, false otherwise.
     */
    boolean isSupported(String operator);
}
