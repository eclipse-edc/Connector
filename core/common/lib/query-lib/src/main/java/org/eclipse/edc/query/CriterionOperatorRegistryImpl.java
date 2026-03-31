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
import org.eclipse.edc.spi.query.CriterionOperator;
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

    private final Map<String, CriterionOperator> operators = new HashMap<>();
    private final List<PropertyLookup> propertyLookups = new ArrayList<>();

    public static CriterionOperatorRegistry ofDefaults() {
        var registry = new CriterionOperatorRegistryImpl();
        registry.registerPropertyLookup(new ReflectionPropertyLookup());
        registry.registerOperator(EQUAL, Object.class, new EqualOperatorPredicate());
        registry.registerOperator(NOT_EQUAL, Object.class, not(new EqualOperatorPredicate()));
        registry.registerOperator(IN, Iterable.class, InOperatorPredicate.in());
        registry.registerOperator(NOT_IN, Iterable.class, InOperatorPredicate.notIn());
        registry.registerOperator(LIKE, Object.class, new LikeOperatorPredicate());
        registry.registerOperator(ILIKE, Object.class, new IlikeOperatorPredicate());
        registry.registerOperator(CONTAINS, Object.class, new ContainsOperatorPredicate());
        registry.registerOperator(LESS_THAN, Object.class, NumberStringOperatorPredicate.lessThan());
        registry.registerOperator(LESS_THAN_EQUAL, Object.class, NumberStringOperatorPredicate.lessThanEqual());
        registry.registerOperator(GREATER_THAN, Object.class, NumberStringOperatorPredicate.greaterThan());
        registry.registerOperator(GREATER_THAN_EQUAL, Object.class, NumberStringOperatorPredicate.greaterThanEqual());
        return registry;
    }

    @Override
    public void registerOperator(String operator, Class<?> rightOperandType, OperatorPredicate predicate) {
        operators.put(operator.toLowerCase(), new CriterionOperator(operator.toLowerCase(), rightOperandType, predicate));
    }

    @Override
    public void registerPropertyLookup(PropertyLookup propertyLookup) {
        propertyLookups.add(0, propertyLookup);
    }

    @Override
    public void unregister(String operator) {
        operators.remove(operator.toLowerCase());
    }

    @Override
    public <T> Predicate<T> toPredicate(Criterion criterion) {
        var operator = operators.get(criterion.getOperator().toLowerCase());
        if (operator == null) {
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

            return operator.predicate().test(property, criterion.getOperandRight());
        };

    }

    @Override
    public boolean isSupported(String operator) {
        return operators.containsKey(operator.toLowerCase());
    }

    @Override
    public CriterionOperator get(String representation) {
        return operators.get(representation.toLowerCase());
    }

}
