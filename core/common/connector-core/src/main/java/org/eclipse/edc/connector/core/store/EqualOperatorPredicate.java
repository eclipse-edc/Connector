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

import org.eclipse.edc.spi.query.OperatorPredicate;

import java.util.List;
import java.util.Objects;

public class EqualOperatorPredicate implements OperatorPredicate {

    @Override
    public boolean test(Object property, Object operandRight) {
        if (property.getClass().isEnum()) {
            var enumProperty = (Enum<?>) property;
            if (operandRight instanceof String) {
                return Objects.equals(enumProperty.name(), operandRight);
            } else if (operandRight instanceof Number) {
                return Objects.equals(enumProperty.ordinal(), operandRight);
            }
        }

        if (property instanceof Number c1 && operandRight instanceof Number c2) {
            // interpret as double to not lose any precision
            return Double.compare(c1.doubleValue(), c2.doubleValue()) == 0;
        }

        if (property instanceof List<?> list) {
            return list.stream().anyMatch(it -> Objects.equals(it, operandRight));
        }

        return Objects.equals(property, operandRight);
    }
}
