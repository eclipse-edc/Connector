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

import org.eclipse.edc.spi.entity.StateResolver;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.query.CriteriaToPredicate;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;

import java.util.List;
import java.util.function.Predicate;

import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * Convert criteria to predicate for stateful entities.
 *
 * @param <E> entity type
 */
public class StatefulEntityCriteriaToPredicate<E extends StatefulEntity<E>> implements CriteriaToPredicate<E> {

    private final CriterionOperatorRegistry criterionOperatorRegistry;
    private final StateResolver stateResolver;

    public StatefulEntityCriteriaToPredicate(CriterionOperatorRegistry criterionOperatorRegistry, StateResolver stateResolver) {
        this.criterionOperatorRegistry = criterionOperatorRegistry;
        this.stateResolver = stateResolver;
    }

    @Override
    public Predicate<E> convert(List<Criterion> criteria) {
        return criteria.stream()
                .map(criterion -> {
                    if (criterion.getOperandLeft().equals("state") && criterion.getOperandRight() instanceof String stateString) {
                        return criterion(criterion.getOperandLeft(), criterion.getOperator(), stateResolver.resolve(stateString));
                    }
                    return criterion;
                })
                .map(criterionOperatorRegistry::<E>toPredicate)
                .reduce(x -> true, Predicate::and);
    }
}
