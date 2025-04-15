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

import org.eclipse.edc.spi.query.OperatorPredicate;

import java.util.List;
import java.util.Objects;

import static java.util.Optional.ofNullable;

public class EqualOperatorPredicate implements OperatorPredicate {

    private final OperatorPredicate numberStringOperatorPredicate = NumberStringOperatorPredicate.equal();

    @Override
    public boolean test(Object property, Object operandRight) {
        if (property == null) {
            return operandRight == null;
        }

        if (property.getClass().isEnum()) {
            var enumProperty = (Enum<?>) property;
            if (operandRight instanceof String) {
                return Objects.equals(enumProperty.name(), operandRight);
            } else if (operandRight instanceof Number) {
                return Objects.equals(enumProperty.ordinal(), operandRight);
            }
        }

        if (property instanceof Boolean booleanProperty) {
            return ofNullable(operandRight)
                    .map(Object::toString)
                    .map(Boolean::parseBoolean)
                    .map(booleanOperand -> booleanOperand == booleanProperty)
                    .orElse(false);
        }

        if (property instanceof List<?> list) {
            return list.stream().anyMatch(it -> Objects.equals(it, operandRight));
        }

        return numberStringOperatorPredicate.test(property, operandRight);
    }
}
