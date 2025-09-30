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

package org.eclipse.edc.query;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.OperatorPredicate;
import org.eclipse.edc.spi.query.PropertyLookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static java.lang.String.format;
import static org.eclipse.edc.query.NotOperatorPredicate.not;

/**
 * Implementation for {@link CriterionOperatorRegistry}
 */
public class CriterionOperatorRegistryImpl implements CriterionOperatorRegistry {

    private final Map<String, OperatorPredicate> operatorPredicates = new HashMap<>();
    private final List<PropertyLookup> propertyLookups = new ArrayList<>();

    public static CriterionOperatorRegistry ofDefaults() {
        var registry = new CriterionOperatorRegistryImpl();
        registry.registerPropertyLookup(new ReflectionPropertyLookup());
        registry.registerOperatorPredicate(EQUAL, new EqualOperatorPredicate());
        registry.registerOperatorPredicate(NOT_EQUAL, not(new EqualOperatorPredicate()));
        registry.registerOperatorPredicate(IN, InOperatorPredicate.in());
        registry.registerOperatorPredicate(NOT_IN, InOperatorPredicate.notIn());
        registry.registerOperatorPredicate(LIKE, new LikeOperatorPredicate());
        registry.registerOperatorPredicate(ILIKE, new IlikeOperatorPredicate());
        registry.registerOperatorPredicate(CONTAINS, new ContainsOperatorPredicate());
        registry.registerOperatorPredicate(LESS_THAN, NumberStringOperatorPredicate.lessThan());
        registry.registerOperatorPredicate(LESS_THAN_EQUAL, NumberStringOperatorPredicate.lessThanEqual());
        registry.registerOperatorPredicate(GREATER_THAN, NumberStringOperatorPredicate.greaterThan());
        registry.registerOperatorPredicate(GREATER_THAN_EQUAL, NumberStringOperatorPredicate.greaterThanEqual());
        return registry;
    }

    @Override
    public void registerOperatorPredicate(String operator, OperatorPredicate converter) {
        operatorPredicates.put(operator.toLowerCase(), converter);
    }

    @Override
    public void registerPropertyLookup(PropertyLookup propertyLookup) {
        propertyLookups.add(0, propertyLookup);
    }

    @Override
    public void unregister(String operator) {
        operatorPredicates.remove(operator.toLowerCase());
    }

    @Override
    public <T> Predicate<T> toPredicate(Criterion criterion) {
        var predicate = operatorPredicates.get(criterion.getOperator().toLowerCase());
        if (predicate == null) {
            throw new IllegalArgumentException(format("Operator [%s] is not supported.", criterion.getOperator()));
        }

        return t -> {

            var operandLeft = (String) criterion.getOperandLeft();

            var property = propertyLookups.stream()
                    .map(it -> it.getProperty(operandLeft, t))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            if (property == null) {
                return false;
            }

            return predicate.test(property, criterion.getOperandRight());
        };

    }

    @Override
    public boolean isSupported(String operator) {
        return operatorPredicates.containsKey(operator.toLowerCase());
    }

}
