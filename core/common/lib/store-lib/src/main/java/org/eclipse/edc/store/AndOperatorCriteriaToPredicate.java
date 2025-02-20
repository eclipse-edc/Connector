/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.store;

import org.eclipse.edc.spi.query.CriteriaToPredicate;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;

import java.util.List;
import java.util.function.Predicate;

/**
 * Converts criteria to a Predicate that converts all the criterion to predicate using the passed {@link #criterionOperatorRegistry}
 * and joins them with "and" operator.
 *
 * @param <T> the object type
 */
public class AndOperatorCriteriaToPredicate<T> implements CriteriaToPredicate<T> {

    private final CriterionOperatorRegistry criterionOperatorRegistry;

    public AndOperatorCriteriaToPredicate(CriterionOperatorRegistry criterionOperatorRegistry) {
        this.criterionOperatorRegistry = criterionOperatorRegistry;
    }

    @Override
    public Predicate<T> convert(List<Criterion> criteria) {
        return criteria.stream()
                .map(criterionOperatorRegistry::<T>toPredicate)
                .reduce(x -> true, Predicate::and);
    }
}
