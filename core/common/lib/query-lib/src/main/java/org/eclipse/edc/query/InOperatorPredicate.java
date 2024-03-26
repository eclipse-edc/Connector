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

public class InOperatorPredicate implements OperatorPredicate {

    @Override
    public boolean test(Object property, Object operandRight) {
        if (operandRight instanceof Iterable<?> iterable) {
            for (var value : iterable) {
                if (value.equals(property)) {
                    return true;
                }
            }
            return false;
        } else {
            throw new IllegalArgumentException("Operator IN requires the right-hand operand to be an " + Iterable.class.getName() + " but was " + operandRight.getClass().getName());
        }
    }
}
