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

package org.eclipse.edc.connector.core.store;

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

/**
 * Implementation for {@link CriterionOperatorRegistry}
 */
public class CriterionOperatorRegistryImpl implements CriterionOperatorRegistry {

    private final Map<String, OperatorPredicate> operatorPredicates = new HashMap<>();
    private final List<PropertyLookup> propertyLookups = new ArrayList<>();

    public static CriterionOperatorRegistry ofDefaults() {
        var registry = new CriterionOperatorRegistryImpl();
        registry.registerPropertyLookup(new ReflectionPropertyLookup());
        registry.registerPropertyLookup(new AssetPropertyLookup());
        registry.registerOperatorPredicate(EQUAL, new EqualOperatorPredicate());
        registry.registerOperatorPredicate(IN, new InOperatorPredicate());
        registry.registerOperatorPredicate(LIKE, new LikeOperatorPredicate());
        registry.registerOperatorPredicate(CONTAINS, new ContainsOperatorPredicate());
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
    public boolean isSupported(String operator) {
        return operatorPredicates.containsKey(operator.toLowerCase());
    }

    @Override
    public <T> Predicate<T> convert(Criterion criterion) {
        var converter = operatorPredicates.get(criterion.getOperator().toLowerCase());
        if (converter == null) {
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

            return converter.test(property, criterion.getOperandRight());
        };

    }

}
