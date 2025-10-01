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

import static org.eclipse.edc.query.NotOperatorPredicate.not;

public class InOperatorPredicate implements OperatorPredicate {

    public static OperatorPredicate notIn() {
        return not(new InOperatorPredicate());
    }

    public static OperatorPredicate in() {
        return new InOperatorPredicate();
    }

    private InOperatorPredicate() {
    }

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
            throw new IllegalArgumentException("Operators ['IN', 'NOT IN'] require the right-hand operand to be an " +
                    Iterable.class.getName() + " but was " + operandRight.getClass().getName());
        }
    }
}
